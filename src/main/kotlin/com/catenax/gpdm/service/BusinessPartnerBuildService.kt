package com.catenax.gpdm.service

import com.catenax.gpdm.dto.BusinessPartnerUpdateDto
import com.catenax.gpdm.dto.ChangelogEntryDto
import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.dto.MetadataMappingDto
import com.catenax.gpdm.dto.request.*
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
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

    /**
     * Create new business partner records from [createRequests] and return as [BusinessPartnerResponse]
     */
    @Transactional
    fun upsertBusinessPartners(createRequests: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse>{
        return upsertBusinessPartners(createRequests, emptyList()).map { it.toDto() }
    }

    /**
     * Create new business partner records from [createRequests] and update existing records with [updateRequests]
     */
    @Transactional
    fun upsertBusinessPartners(createRequests: Collection<BusinessPartnerRequest>, updateRequests: Collection<BusinessPartnerUpdateDto>): Collection<BusinessPartner>{
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
        val partner = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = Instant.now()
    }


    private fun createBusinessPartners(requests: Collection<BusinessPartnerRequest>, metadataMap: MetadataMappingDto): Collection<BusinessPartner> {
        val bpns = bpnIssuingService.issueLegalEntities(requests.size)
        val requestBpnPairs = requests.zip(bpns)

        return requestBpnPairs.map { (request, bpn) -> createBusinessPartner(request, bpn, metadataMap) }
    }

    private fun createBusinessPartner(
        dto: BusinessPartnerRequest,
        bpn: String,
        metadataMap: MetadataMappingDto
    ): BusinessPartner{
        val legalForm = if(dto.legalForm != null) metadataMap.legalForms[dto.legalForm]!! else null

        val partner = toEntity(dto, bpn, legalForm)

        return updateBusinessPartner(partner, dto, metadataMap)
    }

   private  fun updateBusinessPartner(
        partner: BusinessPartner,
        request: BusinessPartnerRequest,
        metadataMap: MetadataMappingDto
    ): BusinessPartner{

        partner.names.clear()
        partner.identifiers.clear()
        partner.stati.clear()
        partner.addresses.clear()
        partner.classification.clear()
        partner.bankAccounts.clear()

        partner.legalForm = if(request.legalForm != null) metadataMap.legalForms[request.legalForm]!! else null
        partner.stati.addAll(if(request.status != null) setOf(toEntity(request.status, partner)) else setOf())
        partner.names.addAll(request.names.map { toEntity(it, partner) }.toSet())
        partner.identifiers.addAll(request.identifiers.map { toEntity(it, metadataMap, partner)})
        partner.addresses.addAll(request.addresses.map { createAddress(it, partner) }.toSet())
        partner.classification.addAll(request.profileClassifications.map { toEntity(it, partner) }.toSet())
        partner.bankAccounts.addAll(request.bankAccounts.map { toEntity(it, partner) }.toSet())

        return partner
    }


    private fun createAddress(
        dto: AddressRequest,
        partner: BusinessPartner
    ): Address{
        val address = Address(
            dto.careOf,
            dto.contexts.toMutableSet(),
            dto.country,
            dto.types.toMutableSet(),
            toEntity(dto.version),
            dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates) },
            partner
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

    private fun toEntity(dto: BusinessStatusRequest, partner: BusinessPartner): BusinessStatus{
        return BusinessStatus(dto.officialDenotation, dto.validFrom, dto.validUntil, dto.type, partner)
    }

    private fun toEntity(dto: BankAccountRequest, partner: BusinessPartner): BankAccount{
        return BankAccount(dto.trustScores.toSet(),
            dto.currency,
            dto.internationalBankAccountIdentifier,
            dto.internationalBankIdentifier,
            dto.nationalBankAccountIdentifier,
            dto.nationalBankIdentifier,
            partner
        )
    }

    private fun toEntity(dto: NameRequest, partner: BusinessPartner): Name{
        return Name(dto.value, dto.shortName, dto.type, dto.language, partner)
    }

    private fun toEntity(dto: ClassificationRequest, partner: BusinessPartner): Classification{
        return Classification(dto.value,dto.code, dto.type, partner)
    }

    private fun toEntity(
        dto: IdentifierRequest,
        metadataMap: MetadataMappingDto,
        partner: BusinessPartner): Identifier{
        return toEntity(dto,
            metadataMap.idTypes[dto.type]!!,
            if(dto.status != null) metadataMap.idStatuses[dto.status]!! else null,
            if(dto.issuingBody != null) metadataMap.issuingBodies[dto.issuingBody]!! else null,
            partner)
    }

    private fun toEntity(dto: IdentifierRequest, type: IdentifierType, status: IdentifierStatus?,  issuingBody: IssuingBody?, partner: BusinessPartner): Identifier{
        return Identifier(dto.value, type, status, issuingBody, partner)
    }

    private fun toEntity(dto: AddressVersionRequest): AddressVersion{
        return AddressVersion(dto.characterSet, dto.language)
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate{
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun toEntity(dto: ThoroughfareRequest, address: Address): Thoroughfare{
        return Thoroughfare(dto.value, dto.name, dto.shortName, dto.number, dto.direction, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: LocalityRequest, address: Address): Locality{
        return Locality(dto.value, dto.shortName, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PremiseRequest, address: Address): Premise{
        return Premise(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PostalDeliveryPointRequest, address: Address): PostalDeliveryPoint{
        return PostalDeliveryPoint(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: AdministrativeAreaRequest, address: Address): AdministrativeArea {
       return AdministrativeArea(dto.value, dto.shortName, dto.fipsCode, dto.type, address.version.language, address.country, address)
    }

    private fun toEntity(dto: PostCodeRequest, address: Address): PostCode {
        return PostCode(dto.value, dto.type, address.country, address)
    }
}