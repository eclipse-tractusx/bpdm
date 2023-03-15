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

import com.neovisionaries.i18n.LanguageCode
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
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
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new legal entities" }

        val errors = mutableListOf<ErrorInfo<LegalEntityCreateError>>()
        val validRequests = filterLegalEntityDuplicatesByIdentifier(requests, errors)

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

        return EntitiesWithErrors(validEntities, errors)
    }

    @Transactional
    fun createSites(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.legalEntity })
        val legalEntityMap = legalEntities.associateBy { it.bpn }

        val (validRequests, invalidRequests) = requests.partition { legalEntityMap[it.legalEntity] != null }
        val errors = invalidRequests.map {
            ErrorInfo(SiteCreateError.LegalEntityNotFound, "Site not created: parent legal entity ${it.legalEntity} not found", it.index)
        }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val requestBpnPairs = validRequests.zip(bpnSs)
        val bpnsMap = requestBpnPairs
            .map { (request, bpns) -> Pair(createSite(request.site, bpns, legalEntityMap[request.legalEntity]!!), request.index) }
            .associateBy { (site, _) -> site.bpn }
        val sites = bpnsMap.values.map { (site, _) -> site }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.SITE) })
        siteRepository.saveAll(sites)

        val validEntities = sites.map { it.toUpsertDto(bpnsMap[it.bpn]!!.second) }

        return EntitiesWithErrors(validEntities, errors)
    }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }
        fun isLegalEntityRequest(request: AddressPartnerCreateRequest) = request.parent.startsWith(bpnIssuingService.bpnlPrefix)
        fun isSiteRequest(request: AddressPartnerCreateRequest) = request.parent.startsWith(bpnIssuingService.bpnsPrefix)

        val (legalEntityRequests, otherAddresses) = requests.partition { isLegalEntityRequest(it) }
        val (siteRequests, invalidAddresses) = otherAddresses.partition { isSiteRequest(it) }

        val errors = mutableListOf<ErrorInfo<AddressCreateError>>()
        invalidAddresses.map {
            ErrorInfo(AddressCreateError.BpnNotValid, "Address not created: parent ${it.parent} is not a valid BPNL/BPNS", it.index)
        }.forEach(errors::add)
        val addressResponses = createSiteAddressResponses(siteRequests, errors).toMutableList()
        addressResponses.addAll(createLegalEntityAddressResponses(legalEntityRequests, errors))

        changelogService.createChangelogEntries(addressResponses.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.ADDRESS) })

        return EntitiesWithErrors(addressResponses, errors)
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }
        val metadataMap = metadataMappingService.mapRequests(requests.map { it.properties })

        val bpnsToFetch = requests.map { it.bpn }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)

        val bpnsNotFetched = bpnsToFetch.minus(legalEntities.map { it.bpn }.toSet())
        val errors = bpnsNotFetched.map {
            ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "Legal entity $it not updated: BPNL not found", it)
        }

        val requestByBpnMap = requests.associateBy { it.bpn }
        legalEntities.forEach { updateLegalEntity(it, requestByBpnMap.get(it.bpn)!!.properties, metadataMap) }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.LEGAL_ENTITY) })

        val validEntities = legalEntityRepository.saveAll(legalEntities).map { it.toUpsertDto(null) }

        return EntitiesWithErrors(validEntities, errors)
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val bpnsToFetch = requests.map { it.bpn }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)

        val bpnsNotFetched = bpnsToFetch.minus(sites.map { it.bpn }.toSet())
        val errors = bpnsNotFetched.map {
            ErrorInfo(SiteUpdateError.SiteNotFound, "Site $it not updated: BPNS not found", it)
        }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.SITE) })

        val requestByBpnMap = requests.associateBy { it.bpn }
        sites.forEach { updateSite(it, requestByBpnMap[it.bpn]!!.site) }
        val validEntities = siteRepository.saveAll(sites).map { it.toUpsertDto(null) }

        return EntitiesWithErrors(validEntities, errors)
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val validAddresses = addressPartnerRepository.findDistinctByBpnIn(requests.map { it.bpn })
        val validBpns = validAddresses.map { it.bpn }.toHashSet()
        val errors = requests.filter { !validBpns.contains(it.bpn) }.map {
            ErrorInfo(AddressUpdateError.AddressNotFound, "Address ${it.bpn} not updated: BPNA not found", it.bpn)
        }

        val requestMap = requests.associateBy { it.bpn }
        validAddresses.forEach { updateAddress(it.address, requestMap[it.bpn]!!.properties) }

        changelogService.createChangelogEntries(validAddresses.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.ADDRESS) })

        val addressResponses =  addressPartnerRepository.saveAll(validAddresses).map { it.toPoolDto() }
        return EntitiesWithErrors(addressResponses, errors)
    }

    @Transactional
    fun setBusinessPartnerCurrentness(bpn: String) {
        logger.info { "Updating currentness of business partner $bpn" }
        val partner = legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = createCurrentnessTimestamp()
        legalEntityRepository.save(partner)
    }

    private fun createLegalEntityAddressResponses(requests: Collection<AddressPartnerCreateRequest>,
                                                  errors: MutableList<ErrorInfo<AddressCreateError>>): Collection<AddressPartnerCreateResponse> {

        fun findValidLegalEnities(requests: Collection<AddressPartnerCreateRequest>): Map<String, LegalEntity> {
            val bpnLsToFetch = requests.map { it.parent }
            val legalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
            val bpnl2LegalEntityMap = legalEntities.associateBy { it.bpn }
            return bpnl2LegalEntityMap
        }

        val bpnl2LegalEntityMap = findValidLegalEnities(requests)
        val (validRequests, invalidRequests) = requests.partition { bpnl2LegalEntityMap[it.parent] != null }

        errors.addAll(invalidRequests.map {
            ErrorInfo(AddressCreateError.LegalEntityNotFound, "Address not created: parent legal entity ${it.parent} not found", it.index)
        })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val validAddressesByIndex = validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(request.index, createPartnerAddress(request.properties, bpna, bpnl2LegalEntityMap[request.parent], null)) }
        addressPartnerRepository.saveAll(validAddressesByIndex.map{it.second})
        return validAddressesByIndex.map { it.second.toCreateResponse(it.first) }
    }

    private fun createSiteAddressResponses(requests: Collection<AddressPartnerCreateRequest>,
                                           errors: MutableList<ErrorInfo<AddressCreateError>>): List<AddressPartnerCreateResponse> {

        fun findValidSites(requests: Collection<AddressPartnerCreateRequest>): Map<String, Site> {
            val bpnsToFetch = requests.map { it.parent }
            val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)
            val bpns2SiteMap = sites.associateBy { it.bpn }
            return bpns2SiteMap
        }

        val bpns2SiteMap = findValidSites(requests)
        val (validRequests, invalidRequests) = requests.partition { bpns2SiteMap[it.parent] != null }
        errors.addAll(invalidRequests.map {
            ErrorInfo(AddressCreateError.SiteNotFound, "Address not created: site ${it.parent} not found", it.index)
        })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val validAddressesByIndex = validRequests
            .zip(bpnAs)
            .map { (request, bpna) -> Pair(request.index, createPartnerAddress(request.properties, bpna, null, bpns2SiteMap[request.parent])) }

        addressPartnerRepository.saveAll(validAddressesByIndex.map{it.second})
        return validAddressesByIndex.map { it.second.toCreateResponse(it.first) }
    }

    private fun createLegalEntity(
        request: LegalEntityDto,
        bpnL: String,
        metadataMap: MetadataMappingDto
    ): LegalEntity {
        val legalName = toEntity(request.legalName)
        val legalForm = request.legalForm?.let { metadataMap.legalForms[it]!! }
        val legalAddress = createAddress(request.legalAddress)

        val partner = LegalEntity(
            bpn = bpnL,
            legalName = legalName,
            legalForm = legalForm,
            currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
            legalAddress = legalAddress
        )

        return updateLegalEntity(partner, request, metadataMap)
    }

    private fun createSite(
        request: SiteDto,
        bpnS: String,
        partner: LegalEntity
    ): Site {
        val mainAddress = createAddress(request.mainAddress)

        val site = Site(
            bpn = bpnS,
            name = request.name,
            legalEntity = partner,
            mainAddress = mainAddress
        )

        site.states.addAll(request.states.map { toEntity(it, site) })

        return site
    }


    private fun updateLegalEntity(
        partner: LegalEntity,
        request: LegalEntityDto,
        metadataMap: MetadataMappingDto
    ): LegalEntity {

        partner.currentness = createCurrentnessTimestamp()

        partner.legalName = toEntity(request.legalName)
        partner.legalForm = request.legalForm?.let { metadataMap.legalForms[it]!! }

        partner.identifiers.clear()
        partner.states.clear()
        partner.classifications.clear()

        partner.states.addAll(request.states.map { toEntity(it, partner) })
        partner.identifiers.addAll(request.identifiers.map { toEntity(it, metadataMap, partner) })
        partner.classifications.addAll(request.classifications.map { toEntity(it, partner) }.toSet())

        updateAddress(partner.legalAddress, request.legalAddress)

        return partner
    }

    private fun updateSite(site: Site, request: SiteDto): Site {
        site.name = request.name

        site.states.clear()

        site.states.addAll(request.states.map { toEntity(it, site) })

        updateAddress(site.mainAddress, request.mainAddress)

        return site
    }

    private fun createAddress(
        dto: LogisticAddressDto
    ): Address {
        val address = Address(
            // TODO map dto
            careOf = "dto.careOf",
            contexts = mutableSetOf(),
            country = dto.postalAddress.country,
            types =  mutableSetOf(),
            version = AddressVersion(CharacterSet.CHINESE,LanguageCode.aa),
            geoCoordinates = dto.postalAddress.geographicCoordinates?.let { toEntity(dto.postalAddress.geographicCoordinates!!) }
        )

        return updateAddress(address, dto)
    }

    private fun createPartnerAddress(
        dto: LogisticAddressDto,
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

    private fun updateAddress(address: Address, dto: LogisticAddressDto): Address{
        //TODO update addresses
//        address.careOf = dto.careOf
//        address.country = dto.country
//        address.geoCoordinates =  dto.geographicCoordinates?.let { toEntity(dto.geographicCoordinates!!) }

        address.administrativeAreas.clear()
        address.postCodes.clear()
        address.thoroughfares.clear()
        address.localities.clear()
        address.premises.clear()
        address.postalDeliveryPoints.clear()
        address.contexts.clear()
        address.types.clear()

//        address.administrativeAreas.addAll(dto.administrativeAreas.map { toEntity(it, address) }.toSet())
//        address.postCodes.addAll(dto.postCodes.map { toEntity(it, address) }.toSet())
//        address.thoroughfares.addAll(dto.thoroughfares.map { toEntity(it, address) }.toSet())
//        address.localities.addAll(dto.localities.map { toEntity(it, address) }.toSet())
//        address.premises.addAll(dto.premises.map { toEntity(it, address) }.toSet())
//        address.postalDeliveryPoints.addAll(dto.postalDeliveryPoints.map { toEntity(it, address) }.toSet())
//        address.contexts.addAll(dto.contexts)
//        address.types.addAll(dto.types)

        return address
    }

    private fun toEntity(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
        return LegalEntityState(
            officialDenotation = dto.officialDenotation,
            validFrom = dto.validFrom,
            validTo = dto.validTo,
            type = dto.type,
            legalEntity = legalEntity
        )
    }

    private fun toEntity(dto: SiteStateDto, site: Site): SiteState {
        return SiteState(
            description = dto.description,
            validFrom = dto.validFrom,
            validTo = dto.validTo,
            type = dto.type,
            site = site
        )
    }

    private fun toEntity(dto: NameDto): Name {
        return Name(
            value = dto.value,
            shortName = dto.shortName
        )
    }

    private fun toEntity(dto: ClassificationDto, partner: LegalEntity): Classification {
        return Classification(
            value = dto.value,
            code = dto.code,
            type = dto.type,
            legalEntity = partner
        )
    }

    private fun toEntity(
        dto: IdentifierDto,
        metadataMap: MetadataMappingDto,
        partner: LegalEntity
    ): Identifier {
        return Identifier(
            value = dto.value,
            type = metadataMap.idTypes[dto.type]!!,
            issuingBody = dto.issuingBody,
            legalEntity = partner
        )
    }

    private fun toEntity(dto: AddressVersionDto): AddressVersion {
        return AddressVersion(dto.characterSet, dto.language)
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate {
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun filterLegalEntityDuplicatesByIdentifier(
        requests: Collection<LegalEntityPartnerCreateRequest>, errors: MutableList<ErrorInfo<LegalEntityCreateError>>): Collection<LegalEntityPartnerCreateRequest> {

        val idValues = requests.flatMap { it.properties.identifiers }.map { it.value }
        val idsInDb = identifierRepository.findByValueIn(idValues).map { Pair(it.value, it.type.technicalKey) }.toHashSet()

        val (invalidRequests, validRequests) = requests.partition {
            it.properties.identifiers.map { id -> Pair(id.value, id.type) }.any { id -> idsInDb.contains(id) }
        }

        invalidRequests.map { 
            ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "Legal entity not created: duplicate identifier", it.index)
        }.forEach(errors::add)

        return validRequests
    }

    private fun createCurrentnessTimestamp(): Instant{
        return Instant.now().truncatedTo(ChronoUnit.MICROS)
    }
}