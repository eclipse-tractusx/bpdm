package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.BusinessPartnerUpdateDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.pool.dto.MetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.dto.request.*
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.BusinessPartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for creating and updating business partner records
 */
@Service
class BusinessPartnerBuildService(
    private val bpnIssuingService: BpnIssuingService,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val metadataMappingService: MetadataMappingService,
    private val changelogService: PartnerChangelogService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Create new business partner records from [createRequests] and return as [BusinessPartnerResponse]
     */
    @Transactional
    fun upsertBusinessPartners(createRequests: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse> {
        return upsertBusinessPartners(createRequests, emptyList()).map { it.toDto() }
    }

    /**
     * Create new business partner records from [createRequests] and update existing records with [updateRequests]
     */
    @Transactional
    fun upsertBusinessPartners(
        createRequests: Collection<BusinessPartnerRequest>,
        updateRequests: Collection<BusinessPartnerUpdateDto>
    ): Collection<BusinessPartner> {
        logger.info { "Create ${createRequests.size} new business partners, and update ${updateRequests.size}" }

        val allRequests = updateRequests.map { (_, request) -> request }.plus(createRequests)
        val metadataMap = metadataMappingService.mapRequests(allRequests)

        val newPartners = createBusinessPartners(createRequests, metadataMap)
        val updatedPartners = updateRequests.map { (partner, request) -> updateBusinessPartner(partner, request, metadataMap) }

        val allPartners = newPartners + updatedPartners

        bpnIssuingService.addIdentifiers(allPartners)
        val createdPartnerLogs = newPartners.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE) }
        val updatedPartnerLogs = updatedPartners.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE) }

        changelogService.createChangelogEntries(createdPartnerLogs + updatedPartnerLogs)

        return businessPartnerRepository.saveAll(allPartners).toList()
    }

    @Transactional
    fun setBusinessPartnerCurrentness(bpn: String) {
        logger.info { "Updating currentness of business partner $bpn" }
        val partner = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = Instant.now()
    }


    private fun createBusinessPartners(requests: Collection<BusinessPartnerRequest>, metadataMap: MetadataMappingDto): Collection<BusinessPartner> {

        val bpns = bpnIssuingService.issueLegalEntityBpns(requests.size)
        val requestBpnPairs = requests.zip(bpns)

        return requestBpnPairs.map { (request, bpn) -> createBusinessPartner(request, bpn, metadataMap) }
    }

    private fun createBusinessPartner(
        dto: BusinessPartnerRequest,
        bpn: String,
        metadataMap: MetadataMappingDto
    ): BusinessPartner {
        val legalForm = if(dto.legalForm != null) metadataMap.legalForms[dto.legalForm]!! else null

        val partner = toEntity(dto, bpn, legalForm)

        return updateBusinessPartner(partner, dto, metadataMap)
    }

    private fun updateBusinessPartner(
        partner: BusinessPartner,
        request: BusinessPartnerRequest,
        metadataMap: MetadataMappingDto
    ): BusinessPartner {
        partner.names.clear()
        partner.identifiers.clear()
        partner.stati.clear()
        partner.addresses.clear()
        partner.classification.clear()
        partner.bankAccounts.clear()

        partner.legalForm = if (request.legalForm != null) metadataMap.legalForms[request.legalForm]!! else null
        partner.stati.addAll(if (request.status != null) setOf(toEntity(request.status, partner)) else setOf())
        partner.names.addAll(request.names.map { toEntity(it, partner) }.toSet())
        partner.identifiers.addAll(request.identifiers.map { toEntity(it, metadataMap, partner) })
        partner.classification.addAll(request.profileClassifications.map { toEntity(it, partner) }.toSet())
        partner.bankAccounts.addAll(request.bankAccounts.map { toEntity(it, partner) }.toSet())

        val addressBpns = bpnIssuingService.issueAddressBpns(request.addresses.size)
        val addressBpnPairs = request.addresses.zip(addressBpns)
        partner.addresses.addAll(addressBpnPairs.map { (request, bpn) -> createAddress(request, bpn, partner, null) })

        val siteBpns = bpnIssuingService.issueSiteBpns(request.sites.size)
        val siteBpnPairs = request.sites.zip(siteBpns)
        partner.sites.addAll(siteBpnPairs.map { (request, bpn) -> createSite(request, bpn, partner) })

        return partner
    }

    private fun createSite(
        dto: SiteRequest,
        bpn: String,
        partner: BusinessPartner
    ): Site {
        val site = Site(bpn, dto.name, partner)

        val addressBpns = bpnIssuingService.issueAddressBpns(dto.addresses.size)
        val addressBpnPairs = dto.addresses.zip(addressBpns)
        site.addresses.addAll(addressBpnPairs.map { (request, bpn) -> createAddress(request, bpn, null, site) })

        return site
    }


    private fun createAddress(
        dto: AddressRequest,
        bpn: String,
        partner: BusinessPartner?,
        site: Site?
    ): Address {
        val address = Address(
            bpn,
            dto.careOf,
            dto.contexts.toMutableSet(),
            dto.country,
            dto.types.toMutableSet(),
            toEntity(dto.version),
            dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates) },
            partner,
            site
        )

        address.administrativeAreas.clear()
        address.postCodes.clear()
        address.thoroughfares.clear()
        address.localities.clear()
        address.premises.clear()
        address.postalDeliveryPoints.clear()

        address.administrativeAreas.addAll(dto.administrativeAreas.map { toEntity(it, address) }.toSet())
        address.postCodes.addAll(dto.postCodes.map { toEntity(it, address) }.toSet())
        address.thoroughfares.addAll(dto.thoroughfares.map { toEntity(it, address) }.toSet())
        address.localities.addAll(dto.localities.map { toEntity(it, address) }.toSet())
        address.premises.addAll(dto.premises.map { toEntity(it, address) }.toSet())
        address.postalDeliveryPoints.addAll(dto.postalDeliveryPoints.map { toEntity(it, address) }.toSet())

        return address
    }

    private fun toEntity(dto: BusinessPartnerRequest, bpn: String, legalForm: LegalForm?): BusinessPartner {
        return BusinessPartner(bpn, legalForm, dto.types.toSet(), emptySet(), Instant.now())
    }

    private fun toEntity(dto: BusinessStatusDto, partner: BusinessPartner): BusinessStatus {
        return BusinessStatus(dto.officialDenotation, dto.validFrom, dto.validUntil, dto.type, partner)
    }

    private fun toEntity(dto: BankAccountDto, partner: BusinessPartner): BankAccount {
        return BankAccount(
            dto.trustScores.toSet(),
            dto.currency,
            dto.internationalBankAccountIdentifier,
            dto.internationalBankIdentifier,
            dto.nationalBankAccountIdentifier,
            dto.nationalBankIdentifier,
            partner
        )
    }

    private fun toEntity(dto: NameDto, partner: BusinessPartner): Name {
        return Name(dto.value, dto.shortName, dto.type, dto.language, partner)
    }

    private fun toEntity(dto: ClassificationDto, partner: BusinessPartner): Classification {
        return Classification(dto.value, dto.code, dto.type, partner)
    }

    private fun toEntity(
        dto: IdentifierDto,
        metadataMap: MetadataMappingDto,
        partner: BusinessPartner
    ): Identifier {
        return toEntity(dto,
            metadataMap.idTypes[dto.type]!!,
            if(dto.status != null) metadataMap.idStatuses[dto.status]!! else null,
            if(dto.issuingBody != null) metadataMap.issuingBodies[dto.issuingBody]!! else null,
            partner)
    }

    private fun toEntity(dto: IdentifierDto, type: IdentifierType, status: IdentifierStatus?, issuingBody: IssuingBody?, partner: BusinessPartner): Identifier {
        return Identifier(dto.value, type, status, issuingBody, partner)
    }

    private fun toEntity(dto: AddressVersionRequest): AddressVersion {
        return AddressVersion(dto.characterSet, dto.language)
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate {
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun toEntity(dto: ThoroughfareRequest, address: Address): Thoroughfare {
        return Thoroughfare(dto.value, dto.name, dto.shortName, dto.number, dto.direction, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: LocalityRequest, address: Address): Locality {
        return Locality(dto.value, dto.shortName, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PremiseRequest, address: Address): Premise {
        return Premise(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PostalDeliveryPointRequest, address: Address): PostalDeliveryPoint {
        return PostalDeliveryPoint(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: AdministrativeAreaRequest, address: Address): AdministrativeArea {
       return AdministrativeArea(dto.value, dto.shortName, dto.fipsCode, dto.type, address.version.language, address.country, address)
    }

    private fun toEntity(dto: PostCodeRequest, address: Address): PostCode {
        return PostCode(dto.value, dto.type, address.country, address)
    }
}