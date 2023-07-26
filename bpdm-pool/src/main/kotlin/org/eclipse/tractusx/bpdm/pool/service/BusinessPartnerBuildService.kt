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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
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
    private val metadataService: MetadataService,
    private val changelogService: PartnerChangelogService,
    private val siteRepository: SiteRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val requestValidationService: RequestValidationService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Create new business partner records from [requests]
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new legal entities" }

        val errorsByRequest = requestValidationService.validateLegalEntityCreates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val legalEntityMetadataMap = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()
        val addressMetadataMap = metadataService.getMetadata(requests.map { it.legalAddress }).toMapping()

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val requestsByLegalEntities = validRequests
            .mapIndexed { bpnIndex, request ->
                val legalEntity = createLegalEntity(request.legalEntity, bpnLs[bpnIndex], request.legalName, legalEntityMetadataMap)
                val legalAddress = createLogisticAddress(request.legalAddress, bpnAs[bpnIndex], legalEntity, addressMetadataMap)
                legalEntity.legalAddress = legalAddress
                Pair(legalEntity, request)
            }
            .toMap()

        val legalEntities = requestsByLegalEntities.keys

        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY)
        })
        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.legalAddress.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        legalEntityRepository.saveAll(legalEntities)

        val legalEntityResponse = legalEntities.map { it.toUpsertDto(requestsByLegalEntities[it]!!.index) }

        return LegalEntityPartnerCreateResponseWrapper(legalEntityResponse, errors)
    }

    @Transactional
    fun createSites(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites" }

        val errorsByRequest = requestValidationService.validateSiteCreates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val validMainAddresses = validRequests.map { it.site.mainAddress }
        val addressMetadataMap = metadataService.getMetadata(validMainAddresses).toMapping()

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(validRequests.map { it.bpnlParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        fun createSiteWithMainAddress(bpnIndex: Int, request: SitePartnerCreateRequest) =
            createSite(request.site, bpnSs[bpnIndex], legalEntitiesByBpn[request.bpnlParent]!!)
                .apply { mainAddress = createLogisticAddress(request.site.mainAddress, bpnAs[bpnIndex], this, addressMetadataMap) }
                .let { site -> Pair(site, request) }

        val requestsBySites = validRequests
            .mapIndexed { i, request -> createSiteWithMainAddress(i, request) }
            .toMap()

        val sites = requestsBySites.keys

        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.mainAddress.bpn, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        siteRepository.saveAll(sites)

        val siteResponse = sites.map { it.toUpsertDto(requestsBySites[it]!!.index) }

        return SitePartnerCreateResponseWrapper(siteResponse, errors)
    }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }

        val errorsByRequest = requestValidationService.validateAddressCreates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        fun isLegalEntityRequest(request: AddressPartnerCreateRequest) =
            bpnIssuingService.translateToBusinessPartnerType(request.bpnParent) == BusinessPartnerType.LEGAL_ENTITY

        fun isSiteRequest(request: AddressPartnerCreateRequest) =
            bpnIssuingService.translateToBusinessPartnerType(request.bpnParent) == BusinessPartnerType.SITE

        val legalEntityRequests = validRequests.filter { isLegalEntityRequest(it) }
        val siteRequests = validRequests.filter { isSiteRequest(it) }

        val metadataMap = metadataService.getMetadata(validRequests.map { it.address }).toMapping()

        val addressResponses = createAddressesForSite(siteRequests, metadataMap)
            .plus(createAddressesForLegalEntity(legalEntityRequests, metadataMap))

        changelogService.createChangelogEntries(addressResponses.map {
            ChangelogEntryCreateRequest(it.address.bpna, ChangelogType.CREATE, BusinessPartnerType.ADDRESS)
        })

        return AddressPartnerCreateResponseWrapper(addressResponses, errors)
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }

        val errorsByRequest = requestValidationService.validateLegalEntityUpdates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val legalEntityMetadataMap = metadataService.getMetadata(validRequests.map { it.legalEntity }).toMapping()
        val addressMetadataMap = metadataService.getMetadata(validRequests.map { it.legalAddress }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpnl }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)

        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY)
        })
        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.legalAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
        })

        val requestsByBpn = validRequests.associateBy { it.bpnl }
        val updatedLegalEntities = legalEntities.map { legalEntity ->
            val request = requestsByBpn[legalEntity.bpn]!!
            updateLegalEntity(legalEntity, request.legalEntity, request.legalName, legalEntityMetadataMap)
            updateLogisticAddress(legalEntity.legalAddress, request.legalAddress, addressMetadataMap)
            legalEntityRepository.save(legalEntity)
        }

        val legalEntityResponses = updatedLegalEntities.map { it.toUpsertDto(it.bpn) }

        return LegalEntityPartnerUpdateResponseWrapper(legalEntityResponses, errors)
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val errorsByRequest = requestValidationService.validateSiteUpdates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val addressMetadataMap = metadataService.getMetadata(requests.map { it.site.mainAddress }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpns }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)

        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)
        })
        changelogService.createChangelogEntries(sites.map {
            ChangelogEntryCreateRequest(it.mainAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
        })

        val requestByBpnMap = validRequests.associateBy { it.bpns }
        val updatedSites = sites.map {
            val request = requestByBpnMap[it.bpn]!!
            updateSite(it, request.site)
            updateLogisticAddress(it.mainAddress, request.site.mainAddress, addressMetadataMap)
            siteRepository.save(it)
        }

        val siteResponses = updatedSites.map { it.toUpsertDto(it.bpn) }

        return SitePartnerUpdateResponseWrapper(siteResponses, errors)
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val errorsByRequest = requestValidationService.validateAddressUpdates(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val addresses = logisticAddressRepository.findDistinctByBpnIn(validRequests.map { it.bpna })
        val metadataMap = metadataService.getMetadata(validRequests.map { it.address }).toMapping()

        changelogService.createChangelogEntries(addresses.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)
        })

        val requestsByBpn = validRequests.associateBy { it.bpna }
        val updatedAddresses = addresses.map { address ->
            updateLogisticAddress(address, requestsByBpn[address.bpn]!!.address, metadataMap)
            logisticAddressRepository.save(address)
        }
        val addressResponses = updatedAddresses.map { it.toDto() }

        return AddressPartnerUpdateResponseWrapper(addressResponses, errors)
    }

    @Transactional
    fun setBusinessPartnerCurrentness(bpn: String) {
        logger.info { "Updating currentness of business partner $bpn" }
        val partner = legalEntityRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        partner.currentness = createCurrentnessTimestamp()
        legalEntityRepository.save(partner)
    }

    private fun createAddressesForLegalEntity(
        validRequests: Collection<AddressPartnerCreateRequest>,
        metadataMap: AddressMetadataMapping
    ): Collection<AddressPartnerCreateVerboseDto> {

        val bpnLsToFetch = validRequests.map { it.bpnParent }
        val parentLegalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
        val parentLegalEntitiesByBpn = parentLegalEntities.associateBy { it.bpn }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val legalEntity = parentLegalEntitiesByBpn[request.bpnParent]!!
                val address = createLogisticAddress(request.address, bpnAs[i], legalEntity, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) -> address.toCreateResponse(index) }
    }

    private fun createAddressesForSite(
        validRequests: Collection<AddressPartnerCreateRequest>,
        metadataMap: AddressMetadataMapping
    ): List<AddressPartnerCreateVerboseDto> {

        val bpnsToFetch = validRequests.map { it.bpnParent }
        val siteParents = siteRepository.findDistinctByBpnIn(bpnsToFetch)
        val siteParentsByBpn = siteParents.associateBy { it.bpn }

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val site = siteParentsByBpn[request.bpnParent]!!
                val address = createLogisticAddress(request.address, bpnAs[i], site, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) -> address.toCreateResponse(index) }
    }

    private fun createLegalEntity(
        request: LegalEntityDto,
        bpnL: String,
        legalNameValue: String,
        metadataMap: LegalEntityMetadataMapping
    ): LegalEntity {
        val legalName = Name(
            value = legalNameValue,
            shortName = request.legalShortName
        )
        val legalForm = request.legalForm?.let { metadataMap.legalForms[it]!! }

        val partner = LegalEntity(
            bpn = bpnL,
            legalName = legalName,
            legalForm = legalForm,
            currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
        )

        updateLegalEntity(partner, request, legalNameValue, metadataMap)

        return partner
    }

    private fun createSite(
        request: SiteDto,
        bpnS: String,
        partner: LegalEntity
    ): Site {
        val site = Site(
            bpn = bpnS,
            name = request.name,
            legalEntity = partner,
        )

        site.states.addAll(request.states.map { toEntity(it, site) })

        return site
    }


    private fun updateLegalEntity(
        partner: LegalEntity,
        request: LegalEntityDto,
        legalName: String,
        metadataMap: LegalEntityMetadataMapping
    ) {

        partner.currentness = createCurrentnessTimestamp()

        partner.legalName = Name(
            value = legalName,
            shortName = request.legalShortName
        )

        partner.legalForm = request.legalForm?.let { metadataMap.legalForms[it]!! }

        partner.identifiers.clear()
        partner.states.clear()
        partner.classifications.clear()

        partner.states.addAll(request.states.map { toEntity(it, partner) })
        partner.identifiers.addAll(request.identifiers.map { toEntity(it, metadataMap, partner) })
        partner.classifications.addAll(request.classifications.map { toEntity(it, partner) }.toSet())
    }

    private fun updateSite(site: Site, request: SiteDto) {
        site.name = request.name

        site.states.clear()
        site.states.addAll(request.states.map { toEntity(it, site) })
    }

    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        legalEntity: LegalEntity,
        metadataMap: AddressMetadataMapping
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .also { it.legalEntity = legalEntity }

    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        site: Site,
        metadataMap: AddressMetadataMapping
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .also { it.site = site }

    private fun createLogisticAddressInternal(
        dto: LogisticAddressDto,
        bpn: String,
        metadataMap: AddressMetadataMapping
    ): LogisticAddress {
        val address = LogisticAddress(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it) },
            name = dto.name
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddress, dto: LogisticAddressDto, metadataMap: AddressMetadataMapping) {
        address.name = dto.name
        address.physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it) }

        address.identifiers.apply {
            clear()
            addAll(dto.identifiers.map { toEntity(it, metadataMap, address) })
        }
        address.states.apply {
            clear()
            addAll(dto.states.map { toEntity(it, address) })
        }
    }

    private fun createPhysicalAddress(physicalAddress: PhysicalPostalAddressDto): PhysicalPostalAddress {
        val baseAddress = physicalAddress.baseAddress
        val area = physicalAddress.areaPart
        return PhysicalPostalAddress(
            geographicCoordinates = baseAddress.geographicCoordinates?.let { toEntity(it) },
            country = baseAddress.country,
            // TODO enable regionCodes later
//            administrativeAreaLevel1 = baseAddress.administrativeAreaLevel1?.let {
//                metadataMap.regions[it] ?: throw BpdmNotFoundException(Region::class, it)
//            },
            administrativeAreaLevel1 = null,
            administrativeAreaLevel2 = area.administrativeAreaLevel2,
            administrativeAreaLevel3 = area.administrativeAreaLevel3,
            postCode = baseAddress.postalCode,
            city = baseAddress.city,
            districtLevel1 = area.district,
            street = physicalAddress.street?.let { createStreet(it) },
            companyPostCode = physicalAddress.basePhysicalAddress.companyPostalCode,
            industrialZone = physicalAddress.basePhysicalAddress.industrialZone,
            building = physicalAddress.basePhysicalAddress.building,
            floor = physicalAddress.basePhysicalAddress.floor,
            door = physicalAddress.basePhysicalAddress.door
        )
    }

    private fun createAlternativeAddress(alternativeAddress: AlternativePostalAddressDto): AlternativePostalAddress {
        val baseAddress = alternativeAddress.baseAddress
        val area = alternativeAddress.areaPart
        return AlternativePostalAddress(
            geographicCoordinates = baseAddress.geographicCoordinates?.let { toEntity(it) },
            country = baseAddress.country,
            // TODO enable regionCodes later
//            administrativeAreaLevel1 = baseAddress.administrativeAreaLevel1?.let {
//                metadataMap.regions[it] ?: throw BpdmNotFoundException(Region::class, it)
//            },
            administrativeAreaLevel1 = null,
            postCode = baseAddress.postalCode,
            city = baseAddress.city,
            deliveryServiceType = alternativeAddress.deliveryServiceType,
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber,
            deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
        )
    }

    private fun createStreet(dto: StreetDto): Street {
        return Street(
            name = dto.name,
            houseNumber = dto.houseNumber,
            milestone = dto.milestone,
            direction = dto.direction
        )
    }

    private fun toEntity(dto: LegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
        return LegalEntityState(
            description = dto.description,
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

    private fun toEntity(dto: AddressStateDto, address: LogisticAddress): AddressState {
        return AddressState(
            description = dto.description,
            validFrom = dto.validFrom,
            validTo = dto.validTo,
            type = dto.type,
            address = address
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
        dto: LegalEntityIdentifierDto,
        metadataMap: LegalEntityMetadataMapping,
        partner: LegalEntity
    ): LegalEntityIdentifier {
        return LegalEntityIdentifier(
            value = dto.value,
            type = metadataMap.idTypes[dto.type]!!,
            issuingBody = dto.issuingBody,
            legalEntity = partner
        )
    }

    private fun toEntity(
        dto: AddressIdentifierDto,
        metadataMap: AddressMetadataMapping,
        partner: LogisticAddress
    ): AddressIdentifier {
        return AddressIdentifier(
            value = dto.value,
            type = metadataMap.idTypes[dto.type]!!,
            address = partner
        )
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate {
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun createCurrentnessTimestamp(): Instant {
        return Instant.now().truncatedTo(ChronoUnit.MICROS)
    }

    private fun LegalEntityMetadataDto.toMapping() =
        LegalEntityMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            legalForms = legalForms.associateBy { it.technicalKey }
        )

    private fun AddressMetadataDto.toMapping() =
        AddressMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            regions = regions.associateBy { it.regionCode }
        )


    private data class LegalEntityMetadataMapping(
        val idTypes: Map<String, IdentifierType>,
        val legalForms: Map<String, LegalForm>
    )

    private data class AddressMetadataMapping(
        val idTypes: Map<String, IdentifierType>,
        val regions: Map<String, Region>
    )
}