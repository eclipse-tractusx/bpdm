/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.PoolErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.MetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

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
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): EntitiesWithErrorsResponse<LegalEntityPartnerCreateResponse> {
        logger.info { "Create ${requests.size} new legal entities" }

        val errors = mutableListOf<ErrorMessageResponse>()
        val validRequests = filterDuplicatesByIdentifier(requests, errors)

        val metadataMap = metadataMappingService.mapRequests(validRequests.map { it.properties })

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val requestWithBpnPairs = validRequests.zip(bpnLs)

        val legalEntityWithIndexByBpnMap = requestWithBpnPairs
            .map { (request, bpnL) -> Pair(createLegalEntity(request.properties, bpnL, metadataMap), request.index) }
            .associateBy { (legalEntity, _) -> legalEntity.bpn }
        val legalEntities = legalEntityWithIndexByBpnMap.values.map { (legalEntity, _) -> legalEntity }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.LEGAL_ENTITY) })
        legalEntityRepository.saveAll(legalEntities)

        val validEntities = legalEntities.map { it.toUpsertDto(legalEntityWithIndexByBpnMap[it.bpn]!!.second) }

        return EntitiesWithErrorsResponse(validEntities, errors)
    }

    @Transactional
    fun createSites(requests: Collection<SitePartnerCreateRequest>): EntitiesWithErrorsResponse<SitePartnerCreateResponse> {
        logger.info { "Create ${requests.size} new sites" }

        val errorTemplate = "Site could not be created: legal entity %s not found"
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.legalEntity })
        val legalEntityMap = legalEntities.associateBy { it.bpn }

        val (validRequests, invalidRequests) = requests.partition { legalEntityMap[it.legalEntity] != null }
        val errors = invalidRequests.map { ErrorMessageResponse(it.index, String.format(errorTemplate,it), PoolErrorCode.legalEntityNotFound) }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val requestBpnPairs = validRequests.zip(bpnSs)
        val bpnsMap = requestBpnPairs
            .map { (request, bpns) -> Pair(createSite(request.site, bpns, legalEntityMap[request.legalEntity]!!), request.index) }
            .associateBy { (site, _) -> site.bpn }
        val sites = bpnsMap.values.map { (site, _) -> site }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.SITE) })
        siteRepository.saveAll(sites)

        val validEntities = sites.map { it.toUpsertDto(bpnsMap[it.bpn]!!.second) }

        return EntitiesWithErrorsResponse(validEntities, errors)
    }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): EntitiesWithErrorsResponse<AddressPartnerCreateResponse> {
        logger.info { "Create ${requests.size} new addresses" }
        fun isLegalEntityRequest(request: AddressPartnerCreateRequest) = request.parent.startsWith(bpnIssuingService.bpnlPrefix)
        fun isSiteRequest(request: AddressPartnerCreateRequest) = request.parent.startsWith(bpnIssuingService.bpnsPrefix)

        val errorTemplate = "Skip address: %s not a valid legal entity or site BPN"
        val (legalEntityRequests, otherAddresses) = requests.partition { isLegalEntityRequest(it) }
        val (siteRequests, invalidAddresses) = otherAddresses.partition { isSiteRequest(it) }

        val errors = invalidAddresses.map { ErrorMessageResponse(it.index, String.format(errorTemplate,it.parent), PoolErrorCode.bpnNotValid) }.toMutableList()
        val addressResponses =  createSiteAddressResponses(siteRequests, errors).toMutableList()
        addressResponses.addAll(createLegalEntityAddressResponses(legalEntityRequests, errors))

        changelogService.createChangelogEntries(addressResponses.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.ADDRESS) })

        return EntitiesWithErrorsResponse(addressResponses, errors)
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): EntitiesWithErrorsResponse<LegalEntityPartnerCreateResponse> {
        logger.info { "Update ${requests.size} legal entities" }
        val metadataMap = metadataMappingService.mapRequests(requests.map { it.properties })
        val errorTemplate = "Legal Entity %s could not be updated: not found"

        val bpnsToFetch = requests.map { it.bpn }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)

        val notFetched = bpnsToFetch.minus(legalEntities.map { it.bpn }.toSet())
        val errors = notFetched.map { ErrorMessageResponse(it, String.format(errorTemplate,it), PoolErrorCode.legalEntityNotFound) }

        val requestByBpnMap = requests.associateBy { it.bpn }
        legalEntities.forEach { updateLegalEntity(it, requestByBpnMap.get(it.bpn)!!.properties, metadataMap) }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.LEGAL_ENTITY) })

        val validEntities = legalEntityRepository.saveAll(legalEntities).map { it.toUpsertDto(null) }

        return EntitiesWithErrorsResponse(validEntities, errors)
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): EntitiesWithErrorsResponse<SitePartnerCreateResponse> {
        logger.info { "Update ${requests.size} sites" }

        val errorTemplate = "Site %s could not be updated: not found"
        val bpnsToFetch = requests.map { it.bpn }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)

        val notFetched = bpnsToFetch.minus(sites.map { it.bpn }.toSet())
        val errors = notFetched.map { ErrorMessageResponse(it, String.format(errorTemplate, it), PoolErrorCode.siteNotFound) }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.SITE) })

        val requestByBpnMap = requests.associateBy { it.bpn }
        sites.forEach { updateSite(it, requestByBpnMap[it.bpn]!!.site) }
        val validEntities = siteRepository.saveAll(sites).map { it.toUpsertDto(null) }

        return EntitiesWithErrorsResponse(validEntities, errors)
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): EntitiesWithErrorsResponse<AddressPartnerResponse> {
        logger.info { "Update ${requests.size} business partner addresses" }
        val errorTemplate = "Address %s could not be updated: not found"

        val validAddresses = addressPartnerRepository.findDistinctByBpnIn(requests.map { it.bpn })
        val validBpns = validAddresses.map { it.bpn }.toHashSet()
        val errors = requests.filter { !validBpns.contains(it.bpn) }.map { ErrorMessageResponse(it.bpn, String.format(errorTemplate,it.bpn), PoolErrorCode.bpnNotValid) }

        val requestMap = requests.associateBy { it.bpn }
        validAddresses.forEach { updateAddress(it.address, requestMap[it.bpn]!!.properties) }

        changelogService.createChangelogEntries(validAddresses.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.ADDRESS) })

        val addressResponses =  addressPartnerRepository.saveAll(validAddresses).map { it.toPoolDto() }
        return EntitiesWithErrorsResponse(addressResponses, errors)
    }

    @Transactional
    fun setBusinessPartnerCurrentness(bpn: String) {
        logger.info { "Updating currentness of business partner $bpn" }
        val partner = legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = createCurrentnessTimestamp()
        legalEntityRepository.save(partner)
    }

    private fun createLegalEntityAddressResponses(requests: Collection<AddressPartnerCreateRequest>
                                                  , errors: MutableList<ErrorMessageResponse>): Collection<AddressPartnerCreateResponse> {

        fun findValidLegalEnities(requests: Collection<AddressPartnerCreateRequest>): Map<String, LegalEntity> {
            val bpnLsToFetch = requests.map { it.parent }
            val legalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
            val bpnl2LegalEntityMap = legalEntities.associateBy { it.bpn }
            return bpnl2LegalEntityMap
        }

        val errorTemplate = "Address could not be created: legal entity %s not found"

        val bpnl2LegalEntityMap = findValidLegalEnities(requests)
        val (validRequests, invalidRequests) = requests.partition { bpnl2LegalEntityMap[it.parent] != null }

        errors.addAll(invalidRequests
            .map { ErrorMessageResponse(it.index,String.format(errorTemplate, it) , PoolErrorCode.legalEntityOfAddressNotFound) })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val validAddressesByIndex = validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(request.index, createPartnerAddress(request.properties, bpna, bpnl2LegalEntityMap[request.parent], null)) }
        addressPartnerRepository.saveAll(validAddressesByIndex.map{it.second})
        return validAddressesByIndex.map { it.second.toCreateResponse(it.first) }
    }

    private fun createSiteAddressResponses(requests: Collection<AddressPartnerCreateRequest>
                                           , errors: MutableList<ErrorMessageResponse>): List<AddressPartnerCreateResponse> {

        fun findValidSites(requests: Collection<AddressPartnerCreateRequest>): Map<String, Site> {
            val bpnsToFetch = requests.map { it.parent }
            val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)
            val bpns2SiteMap = sites.associateBy { it.bpn }
            return bpns2SiteMap
        }

        val errorTemplate = "Address could not be created: site %s not found"
        val bpns2SiteMap = findValidSites(requests)
        val (validRequests, invalidRequests) = requests.partition { bpns2SiteMap[it.parent] != null }
        errors.addAll(invalidRequests
            .map { ErrorMessageResponse(it.index, String.format(errorTemplate, it) , PoolErrorCode.siteOfAddressNotFound) })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val validAddressesByIndex = validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(request.index, createPartnerAddress(request.properties, bpna, null, bpns2SiteMap[request.parent])) }

        addressPartnerRepository.saveAll(validAddressesByIndex.map{it.second})
        return validAddressesByIndex.map { it.second.toCreateResponse(it.first) }
    }

    private fun createLegalEntity(
        dto: LegalEntityDto,
        bpnL: String,
        metadataMap: MetadataMappingDto
    ): LegalEntity {
        val legalForm = if (dto.legalForm != null) metadataMap.legalForms[dto.legalForm]!! else null

        val legalAddress = createAddress(dto.legalAddress)
        val partner = LegalEntity(bpnL, legalForm, dto.types.toMutableSet(), mutableSetOf(), Instant.now().truncatedTo(ChronoUnit.MICROS), legalAddress)

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

        partner.currentness = createCurrentnessTimestamp()

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
            dto.trustScores.toMutableSet(),
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

    private fun filterDuplicatesByIdentifier(
        requests: Collection<LegalEntityPartnerCreateRequest>, errors: MutableList<ErrorMessageResponse>): Collection<LegalEntityPartnerCreateRequest> {
        
        val errorTemplate = "Skip creation of legal entity with index %s: Duplicate identifier"

        val idValues = requests.flatMap { it.properties.identifiers }.map { it.value }
        val idsInDb = identifierRepository.findByValueIn(idValues).map { Pair(it.value, it.type.technicalKey) }.toHashSet()

        val (invalidRequests, validRequests) = requests.partition {
            it.properties.identifiers.map { id -> Pair(id.value, id.type) }.any { id -> idsInDb.contains(id) }
        }

        invalidRequests.map { 
            ErrorMessageResponse(it.index, String.format(errorTemplate, it.index), PoolErrorCode.legalEntityDuplicateIdentifier) 
        }.forEach(errors::add)

        return validRequests
    }

    private fun createCurrentnessTimestamp(): Instant{
        return Instant.now().truncatedTo(ChronoUnit.MICROS)
    }
}