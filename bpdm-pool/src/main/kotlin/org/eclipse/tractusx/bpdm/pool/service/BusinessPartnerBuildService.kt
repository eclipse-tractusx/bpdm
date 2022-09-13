/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.MetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.dto.request.*
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for creating and updating business partner records
 */
@Service
class BusinessPartnerBuildService(
    private val bpnIssuingService: BpnIssuingService,
    private val legalEntityRepository: LegalEntityRepository,
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val metadataMappingService: MetadataMappingService,
    private val changelogService: PartnerChangelogService,
    private val siteRepository: SiteRepository,
    private val addressPartnerRepository: AddressPartnerRepository,
    private val identifierRepository: IdentifierRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Create new business partner records from [requests]
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): Collection<LegalEntityPartnerCreateResponse> {
        logger.info { "Create ${requests.size} new legal entities" }

        val validRequests = filterDuplicatesByIdentifier(requests)

        val metadataMap = metadataMappingService.mapRequests(validRequests.map { it.properties })

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnPairs = validRequests.zip(bpnLs)

        val bpnMap = bpnPairs.map { (request, bpnL) -> Pair(createLegalEntity(request.properties, bpnL, metadataMap), request.index) }
            .associateBy { (legalEntity, _) -> legalEntity.bpn }
        val legalEntities = bpnMap.values.map { (legalEntity, _) -> legalEntity }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE) })
        legalEntityRepository.saveAll(legalEntities)

        return legalEntities.map { it.toUpsertDto(bpnMap[it.bpn]!!.second) }
    }

    @Transactional
    fun createSites(requests: Collection<SitePartnerCreateRequest>): Collection<SitePartnerCreateResponse> {
        logger.info { "Create ${requests.size} new sites" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.legalEntity })
        val legalEntityMap = legalEntities.associateBy { it.bpn }

        val validRequests = requests.filter { legalEntityMap[it.legalEntity] != null }

        if (requests.size != validRequests.size) {
            val invalid = requests.map { it.legalEntity }.minus(validRequests.map { it.legalEntity }.toSet())
            invalid.forEach { logger.warn { "Site could not be created: legal entity $it not found" } }
        }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val requestBpnPairs = validRequests.zip(bpnSs)
        val bpnsMap = requestBpnPairs
            .map { (request, bpns) -> Pair(createSite(request.site, bpns, legalEntityMap[request.legalEntity]!!), request.index) }
            .associateBy { (site, _) -> site.bpn }
        val sites = bpnsMap.values.map { (site, _) -> site }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE) })
        siteRepository.saveAll(sites)

        return sites.map { it.toUpsertDto(bpnsMap[it.bpn]!!.second) }
    }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): Collection<AddressPartnerCreateResponse> {
        logger.info { "Create ${requests.size} new addresses" }

        val (bpnlRequests, otherAddresses) = requests.partition { it.parent.startsWith(bpnIssuingService.bpnlPrefix) }
        val (bpnsRequests, invalidAddresses) = otherAddresses.partition { it.parent.startsWith(bpnIssuingService.bpnsPrefix) }

        invalidAddresses.forEach { logger.warn { "Skip address: ${it.parent} not a valid legal entity or site BPN" } }

        val bpnaMap = createLegalEntityAddresses(bpnlRequests)
            .plus(createSiteAddresses(bpnsRequests))
            .associateBy { (address, _) -> address.bpn }
        val addresses = bpnaMap.values.map { (address, _) -> address }

        changelogService.createChangelogEntries(addresses.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE) })

        return addressPartnerRepository.saveAll(addresses).map { it.toCreateResponse(bpnaMap[it.bpn]!!.second) }
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): Collection<LegalEntityPartnerCreateResponse> {
        logger.info { "Update ${requests.size} legal entities" }
        val metadataMap = metadataMappingService.mapRequests(requests.map { it.properties })

        val bpnsToFetch = requests.map { it.bpn }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)

        if (legalEntities.size != bpnsToFetch.size) {
            val notFetched = bpnsToFetch.minus(legalEntities.map { it.bpn }.toSet())
            notFetched.forEach { logger.warn { "Legal Entity $it could not be updated: not found" } }
        }

        val requestMap = requests.associateBy { it.bpn }
        legalEntities.forEach { updateLegalEntity(it, requestMap.get(it.bpn)!!.properties, metadataMap) }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE) })

        return legalEntityRepository.saveAll(legalEntities).map { it.toUpsertDto(null) }
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): Collection<SitePartnerCreateResponse> {
        logger.info { "Update ${requests.size} sites" }
        val bpnsToFetch = requests.map { it.bpn }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)

        if (sites.size != bpnsToFetch.size) {
            val notFetched = bpnsToFetch.minus(sites.map { it.bpn }.toSet())
            notFetched.forEach { logger.warn { "Site $it could not be updated: not found" } }
        }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE) })

        val requestMap = requests.associateBy { it.bpn }
        sites.forEach { updateSite(it, requestMap[it.bpn]!!.site) }
        return siteRepository.saveAll(sites).map { it.toUpsertDto(null) }
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): Collection<AddressPartnerResponse> {
        logger.info { "Update ${requests.size} business partner addresses" }

        val addresses = addressPartnerRepository.findDistinctByBpnIn(requests.map { it.bpn })

        if (addresses.size != requests.size) {
            val notFetched = requests.map { it.bpn }.minus(addresses.map { it.bpn }.toSet())
            notFetched.forEach { logger.warn { "Address $it could not be updated: not found" } }
        }

        val requestMap = requests.associateBy { it.bpn }
        addresses.forEach { updateAddress(it.address, requestMap[it.bpn]!!.properties) }

        changelogService.createChangelogEntries(addresses.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE) })

        return addressPartnerRepository.saveAll(addresses).map { it.toPoolDto() }
    }

    @Transactional
    fun setBusinessPartnerCurrentness(bpn: String) {
        logger.info { "Updating currentness of business partner $bpn" }
        val partner = legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = Instant.now()
    }

    private fun createLegalEntityAddresses(requests: Collection<AddressPartnerCreateRequest>): Collection<Pair<AddressPartner, String?>> {
        val bpnLsToFetch = requests.map { it.parent }
        val legalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
        val bpnlMap = legalEntities.associateBy { it.bpn }

        val validRequests = requests.filter { bpnlMap[it.parent] != null }

        if (validRequests.size != requests.size) {
            val notFetched = bpnLsToFetch.minus(legalEntities.map { it.bpn }.toSet())
            notFetched.forEach { logger.warn { "Address could not be created: legal entity $it not found" } }
        }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        return validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(createPartnerAddress(request.properties, bpna, bpnlMap[request.parent], null), request.index) }
    }

    private fun createSiteAddresses(requests: Collection<AddressPartnerCreateRequest>): Collection<Pair<AddressPartner, String?>> {
        val bpnsToFetch = requests.map { it.parent }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)
        val bpnsMap = sites.associateBy { it.bpn }

        val validRequests = requests.filter { bpnsMap[it.parent] != null }

        if (validRequests.size != requests.size) {
            val notFetched = bpnsToFetch.minus(sites.map { it.bpn }.toSet())
            notFetched.forEach { logger.warn { "Address could not be created: site $it not found" } }
        }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        return validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(createPartnerAddress(request.properties, bpna, null, bpnsMap[request.parent]), request.index) }
    }

    private fun createLegalEntity(
        dto: LegalEntityDto,
        bpnL: String,
        metadataMap: MetadataMappingDto
    ): LegalEntity {
        val legalForm = if (dto.legalForm != null) metadataMap.legalForms[dto.legalForm]!! else null

        val legalAddress = createAddress(dto.legalAddress)
        val partner = LegalEntity(bpnL, legalForm, dto.types.toSet(), emptySet(), Instant.now(), legalAddress)

        return updateLegalEntity(partner, dto, metadataMap)
    }

    private fun createSite(
        dto: SiteDto,
        bpnS: String,
        partner: LegalEntity
    ): Site {
        val mainAddress = createAddress(dto.mainAddress)
        val site = Site(bpnS, dto.name, partner, mainAddress)

        return site
    }


    private fun updateLegalEntity(
        partner: LegalEntity,
        request: LegalEntityDto,
        metadataMap: MetadataMappingDto
    ): LegalEntity {
        partner.names.clear()
        partner.identifiers.clear()
        partner.stati.clear()
        partner.classification.clear()
        partner.bankAccounts.clear()

        partner.legalForm = if (request.legalForm != null) metadataMap.legalForms[request.legalForm]!! else null
        partner.stati.addAll(if (request.status != null) setOf(toEntity(request.status!!, partner)) else setOf())
        partner.names.addAll(request.names.map { toEntity(it, partner) }.toSet())
        partner.identifiers.addAll(request.identifiers.map { toEntity(it, metadataMap, partner) })
        partner.classification.addAll(request.profileClassifications.map { toEntity(it, partner) }.toSet())
        partner.bankAccounts.addAll(request.bankAccounts.map { toEntity(it, partner) }.toSet())

        updateAddress(partner.legalAddress, request.legalAddress)

        return partner
    }

    private fun updateSite(site: Site, request: SiteDto): Site {
        site.name = request.name

        updateAddress(site.mainAddress, request.mainAddress)

        return site
    }

    private fun createAddress(
        dto: AddressDto
    ): Address {
        val address = Address(
            dto.careOf,
            dto.contexts.toMutableSet(),
            dto.country,
            dto.types.toMutableSet(),
            toEntity(dto.version),
            dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates!!) }
        )

        return updateAddress(address, dto)
    }

    private fun createPartnerAddress(
        dto: AddressDto,
        bpn: String,
        partner: LegalEntity?,
        site: Site?
    ): AddressPartner {
        val addressPartner = AddressPartner(
            bpn,
            partner,
            site,
            createAddress(dto)
        )

        updateAddress(addressPartner.address, dto)

        return addressPartner
    }

    private fun updateAddress(address: Address, dto: AddressDto): Address{
        address.careOf = dto.careOf
        address.country = dto.country
        address.geoCoordinates =  dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates!!) }

        address.administrativeAreas.clear()
        address.postCodes.clear()
        address.thoroughfares.clear()
        address.localities.clear()
        address.premises.clear()
        address.postalDeliveryPoints.clear()
        address.contexts.clear()
        address.types.clear()

        address.administrativeAreas.addAll(dto.administrativeAreas.map { toEntity(it, address) }.toSet())
        address.postCodes.addAll(dto.postCodes.map { toEntity(it, address) }.toSet())
        address.thoroughfares.addAll(dto.thoroughfares.map { toEntity(it, address) }.toSet())
        address.localities.addAll(dto.localities.map { toEntity(it, address) }.toSet())
        address.premises.addAll(dto.premises.map { toEntity(it, address) }.toSet())
        address.postalDeliveryPoints.addAll(dto.postalDeliveryPoints.map { toEntity(it, address) }.toSet())
        address.contexts.addAll(dto.contexts)
        address.types.addAll(dto.types)

        return address
    }

    private fun toEntity(dto: BusinessStatusDto, partner: LegalEntity): BusinessStatus {
        return BusinessStatus(dto.officialDenotation, dto.validFrom, dto.validUntil, dto.type, partner)
    }

    private fun toEntity(dto: BankAccountDto, partner: LegalEntity): BankAccount {
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

    private fun toEntity(dto: NameDto, partner: LegalEntity): Name {
        return Name(dto.value, dto.shortName, dto.type, dto.language, partner)
    }

    private fun toEntity(dto: ClassificationDto, partner: LegalEntity): Classification {
        return Classification(dto.value, dto.code, dto.type, partner)
    }

    private fun toEntity(
        dto: IdentifierDto,
        metadataMap: MetadataMappingDto,
        partner: LegalEntity
    ): Identifier {
        return toEntity(dto,
            metadataMap.idTypes[dto.type]!!,
            if(dto.status != null) metadataMap.idStatuses[dto.status]!! else null,
            if(dto.issuingBody != null) metadataMap.issuingBodies[dto.issuingBody]!! else null,
            partner)
    }

    private fun toEntity(dto: IdentifierDto, type: IdentifierType, status: IdentifierStatus?, issuingBody: IssuingBody?, partner: LegalEntity): Identifier {
        return Identifier(dto.value, type, status, issuingBody, partner)
    }

    private fun toEntity(dto: AddressVersionDto): AddressVersion {
        return AddressVersion(dto.characterSet, dto.language)
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate {
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun toEntity(dto: ThoroughfareDto, address: Address): Thoroughfare {
        return Thoroughfare(dto.value, dto.name, dto.shortName, dto.number, dto.direction, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: LocalityDto, address: Address): Locality {
        return Locality(dto.value, dto.shortName, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PremiseDto, address: Address): Premise {
        return Premise(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: PostalDeliveryPointDto, address: Address): PostalDeliveryPoint {
        return PostalDeliveryPoint(dto.value, dto.shortName, dto.number, dto.type, address.version.language, address)
    }

    private fun toEntity(dto: AdministrativeAreaDto, address: Address): AdministrativeArea {
        return AdministrativeArea(dto.value, dto.shortName, dto.fipsCode, dto.type, address.version.language, address.country, address)
    }

    private fun toEntity(dto: PostCodeDto, address: Address): PostCode {
        return PostCode(dto.value, dto.type, address.country, address)
    }

    private fun filterDuplicatesByIdentifier(requests: Collection<LegalEntityPartnerCreateRequest>): Collection<LegalEntityPartnerCreateRequest> {
        val idValues = requests.flatMap { it.properties.identifiers }.map { it.value }
        val idHash = identifierRepository.findByValueIn(idValues).map { Pair(it.value, it.type.technicalKey) }.toHashSet()

        val invalidRequests = requests.filter { it.properties.identifiers.map { id -> Pair(id.value, id.type) }.any { id -> idHash.contains(id) } }.toSet()

        if (invalidRequests.isNotEmpty()) {
            invalidRequests.forEach { logger.warn { "Skip creation of legal entity with index $it: Duplicate identifier" } }
        }

        return requests.minus(invalidRequests)
    }
}