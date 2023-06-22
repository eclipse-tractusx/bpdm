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
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogSubject
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
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
    private val metadataMappingService: MetadataMappingService,
    private val changelogService: PartnerChangelogService,
    private val siteRepository: SiteRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository
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

        val legalEntityMetadataMap = metadataMappingService.mapRequests(validRequests.map { it.legalEntity })
        val addressMetadataMap = metadataMappingService.mapRequests(validRequests.map { it.legalAddress })

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val legalEntityWithIndexByBpnMap = validRequests
            .mapIndexed { i, request ->
                val legalEntity = createLegalEntity(request.legalEntity, bpnLs[i], request.legalName, legalEntityMetadataMap)
                legalEntity.legalAddress = createLogisticAddress(request.legalAddress, bpnAs[i], legalEntity, addressMetadataMap)
                Pair(legalEntity, request.index)
            }
            .associateBy { (legalEntity, _) -> legalEntity.bpn }

        val legalEntities = legalEntityWithIndexByBpnMap.values.map { (legalEntity, _) -> legalEntity }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.LEGAL_ENTITY) })
        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.legalAddress.bpn, ChangelogType.CREATE, ChangelogSubject.ADDRESS) })

        legalEntityRepository.saveAll(legalEntities)

        val validEntities = legalEntities.map { it.toUpsertDto(legalEntityWithIndexByBpnMap[it.bpn]!!.second) }

        return LegalEntityPartnerCreateResponseWrapper(validEntities, errors)
    }

    @Transactional
    fun createSites(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnlParent })
        val legalEntityMap = legalEntities.associateBy { it.bpn }

        val (validRequests, invalidRequests) = requests.partition { legalEntityMap[it.bpnlParent] != null }
        val errors = invalidRequests.map {
            ErrorInfo(SiteCreateError.LegalEntityNotFound, "Site not created: parent legal entity ${it.bpnlParent} not found", it.index)
        }

        val addressMetadataMap = metadataMappingService.mapRequests(validRequests.map { it.site.mainAddress })

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val siteWithIndexByBpnMap = validRequests
            .mapIndexed { i, request ->
                val legalEntity = legalEntityMap[request.bpnlParent]!!
                val site = createSite(request.site, bpnSs[i], legalEntity)
                site.mainAddress = createLogisticAddress(request.site.mainAddress, bpnAs[i], site, addressMetadataMap)
                Pair(site, request.index)
            }
            .associateBy { (site, _) -> site.bpn }
        val sites = siteWithIndexByBpnMap.values.map { (site, _) -> site }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.CREATE, ChangelogSubject.SITE) })
        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.mainAddress.bpn, ChangelogType.CREATE, ChangelogSubject.ADDRESS) })

        siteRepository.saveAll(sites)

        val validEntities = sites.map { it.toUpsertDto(siteWithIndexByBpnMap[it.bpn]!!.second) }

        return SitePartnerCreateResponseWrapper(validEntities, errors)
    }

    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }
        fun isLegalEntityRequest(request: AddressPartnerCreateRequest) = request.bpnParent.startsWith(bpnIssuingService.bpnlPrefix)
        fun isSiteRequest(request: AddressPartnerCreateRequest) = request.bpnParent.startsWith(bpnIssuingService.bpnsPrefix)

        val (legalEntityRequests, otherAddresses) = requests.partition { isLegalEntityRequest(it) }
        val (siteRequests, invalidAddresses) = otherAddresses.partition { isSiteRequest(it) }

        val validRequests = legalEntityRequests.plus(siteRequests)
        val metadataMap = metadataMappingService.mapRequests(validRequests.map { it.address })

        val errors = mutableListOf<ErrorInfo<AddressCreateError>>()
        invalidAddresses.map {
            ErrorInfo(AddressCreateError.BpnNotValid, "Address not created: parent ${it.bpnParent} is not a valid BPNL/BPNS", it.index)
        }.forEach(errors::add)

        val addressResponses = createAddressesForSite(siteRequests, errors, metadataMap)
            .plus(createAddressesForLegalEntity(legalEntityRequests, errors, metadataMap))

        changelogService.createChangelogEntries(addressResponses.map { ChangelogEntryDto(it.address.bpna, ChangelogType.CREATE, ChangelogSubject.ADDRESS) })

        return AddressPartnerCreateResponseWrapper(addressResponses, errors)
    }

    /**
     * Update existing records with [requests]
     */
    @Transactional
    fun updateLegalEntities(requests: Collection<LegalEntityPartnerUpdateRequest>): LegalEntityPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} legal entities" }

        val legalEntityMetadataMap = metadataMappingService.mapRequests(requests.map { it.legalEntity })
        val addressMetadataMap = metadataMappingService.mapRequests(requests.map { it.legalAddress })

        val bpnsToFetch = requests.map { it.bpnl }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)

        val bpnsNotFetched = bpnsToFetch.minus(legalEntities.map { it.bpn }.toSet())
        val errors = bpnsNotFetched.map {
            ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "Legal entity $it not updated: BPNL not found", it)
        }

        val requestByBpnMap = requests.associateBy { it.bpnl }
        legalEntities.forEach {
            val request = requestByBpnMap.get(it.bpn)!!
            updateLegalEntity(it, request.legalEntity, request.legalName, legalEntityMetadataMap)
            updateLogisticAddress(it.legalAddress, request.legalAddress, addressMetadataMap)
        }

        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.LEGAL_ENTITY) })
        changelogService.createChangelogEntries(legalEntities.map { ChangelogEntryDto(it.legalAddress.bpn, ChangelogType.UPDATE, ChangelogSubject.ADDRESS) })

        val validEntities = legalEntityRepository.saveAll(legalEntities).map { it.toUpsertDto(null) }

        return LegalEntityPartnerUpdateResponseWrapper(validEntities, errors)
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val addressMetadataMap = metadataMappingService.mapRequests(requests.map { it.site.mainAddress })

        val bpnsToFetch = requests.map { it.bpns }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)

        val bpnsNotFetched = bpnsToFetch.minus(sites.map { it.bpn }.toSet())
        val errors = bpnsNotFetched.map {
            ErrorInfo(SiteUpdateError.SiteNotFound, "Site $it not updated: BPNS not found", it)
        }

        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.SITE) })
        changelogService.createChangelogEntries(sites.map { ChangelogEntryDto(it.mainAddress.bpn, ChangelogType.UPDATE, ChangelogSubject.ADDRESS) })

        val requestByBpnMap = requests.associateBy { it.bpns }
        sites.forEach {
            val request = requestByBpnMap[it.bpn]!!
            updateSite(it, request.site)
            updateLogisticAddress(it.mainAddress, request.site.mainAddress, addressMetadataMap)
        }
        val validEntities = siteRepository.saveAll(sites).map { it.toUpsertDto(null) }

        return SitePartnerUpdateResponseWrapper(validEntities, errors)
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val validAddresses = logisticAddressRepository.findDistinctByBpnIn(requests.map { it.bpna })
        val validBpns = validAddresses.map { it.bpn }.toHashSet()
        val (validRequests, invalidRequests) = requests.partition { validBpns.contains(it.bpna) }
        val errors = invalidRequests.map {
            ErrorInfo(AddressUpdateError.AddressNotFound, "Address ${it.bpna} not updated: BPNA not found", it.bpna)
        }

        val requestByBpnMap = requests.associateBy { it.bpna }
        val metadataMap = metadataMappingService.mapRequests(validRequests.map { it.address })

        validAddresses.forEach { updateLogisticAddress(it, requestByBpnMap[it.bpn]!!.address, metadataMap) }

        changelogService.createChangelogEntries(validAddresses.map { ChangelogEntryDto(it.bpn, ChangelogType.UPDATE, ChangelogSubject.ADDRESS) })

        val addressResponses = logisticAddressRepository.saveAll(validAddresses).map { it.toDto() }
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
        requests: Collection<AddressPartnerCreateRequest>,
        errors: MutableList<ErrorInfo<AddressCreateError>>,
        metadataMap: AddressMetadataMappingDto
    ): Collection<AddressPartnerCreateVerboseDto> {

        fun findValidLegalEnities(requests: Collection<AddressPartnerCreateRequest>): Map<String, LegalEntity> {
            val bpnLsToFetch = requests.map { it.bpnParent }
            val legalEntities = businessPartnerFetchService.fetchByBpns(bpnLsToFetch)
            val bpnl2LegalEntityMap = legalEntities.associateBy { it.bpn }
            return bpnl2LegalEntityMap
        }

        val legalEntityByBpnMap = findValidLegalEnities(requests)
        val (validRequests, invalidRequests) = requests.partition { legalEntityByBpnMap[it.bpnParent] != null }

        errors.addAll(invalidRequests.map {
            ErrorInfo(AddressCreateError.LegalEntityNotFound, "Address not created: parent legal entity ${it.bpnParent} not found", it.index)
        })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val legalEntity = legalEntityByBpnMap[request.bpnParent]!!
                val address = createLogisticAddress(request.address, bpnAs[i], legalEntity, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) -> address.toCreateResponse(index) }
    }

    private fun createAddressesForSite(
        requests: Collection<AddressPartnerCreateRequest>,
        errors: MutableList<ErrorInfo<AddressCreateError>>,
        metadataMap: AddressMetadataMappingDto
    ): List<AddressPartnerCreateVerboseDto> {

        val bpnsToFetch = requests.map { it.bpnParent }
        val siteByBpnMap = siteRepository
            .findDistinctByBpnIn(bpnsToFetch)
            .associateBy { it.bpn }

        val (validRequests, invalidRequests) = requests.partition { siteByBpnMap[it.bpnParent] != null }
        errors.addAll(invalidRequests.map {
            ErrorInfo(AddressCreateError.SiteNotFound, "Address not created: site ${it.bpnParent} not found", it.index)
        })

        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)
        val addressesWithIndex = validRequests
            .mapIndexed { i, request ->
                val site = siteByBpnMap[request.bpnParent]!!
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
        metadataMap: LegalEntityMetadataMappingDto
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

        return updateLegalEntity(partner, request, legalNameValue, metadataMap)
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
        metadataMap: LegalEntityMetadataMappingDto
    ): LegalEntity {

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

        return partner
    }

    private fun updateSite(site: Site, request: SiteDto): Site {
        site.name = request.name

        site.states.clear()
        site.states.addAll(request.states.map { toEntity(it, site) })

        return site
    }

    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        legalEntity: LegalEntity,
        metadataMap: AddressMetadataMappingDto
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .also { it.legalEntity = legalEntity }

    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        site: Site,
        metadataMap: AddressMetadataMappingDto
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .also { it.site = site }

    private fun createLogisticAddressInternal(
        dto: LogisticAddressDto,
        bpn: String,
        metadataMap: AddressMetadataMappingDto
    ): LogisticAddress {
        val address = LogisticAddress(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap) },
            name = dto.name
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddress, dto: LogisticAddressDto, metadataMap: AddressMetadataMappingDto) {
        address.name = dto.name
        address.physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap) }

        address.identifiers.apply {
            clear()
            addAll(dto.identifiers.map { toEntity(it, metadataMap, address) })
        }
        address.states.apply {
            clear()
            addAll(dto.states.map { toEntity(it, address) })
        }
    }

    private fun createPhysicalAddress(physicalAddress: PhysicalPostalAddressDto, metadataMap: AddressMetadataMappingDto): PhysicalPostalAddress {
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

    private fun createAlternativeAddress(alternativeAddress: AlternativePostalAddressDto, metadataMap: AddressMetadataMappingDto): AlternativePostalAddress {
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
            deliveryServiceNumber = alternativeAddress.deliveryServiceNumber
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
        metadataMap: LegalEntityMetadataMappingDto,
        partner: LegalEntity
    ): LegalEntityIdentifier {
        return LegalEntityIdentifier(
            value = dto.value,
            type = metadataMap.idTypes[dto.type] ?: throw BpdmNotFoundException(IdentifierType::class, dto.type),
            issuingBody = dto.issuingBody,
            legalEntity = partner
        )
    }

    private fun toEntity(
        dto: AddressIdentifierDto,
        metadataMap: AddressMetadataMappingDto,
        partner: LogisticAddress
    ): AddressIdentifier {
        return AddressIdentifier(
            value = dto.value,
            type = metadataMap.idTypes[dto.type] ?: throw BpdmNotFoundException(IdentifierType::class, dto.type),
            address = partner
        )
    }

    private fun toEntity(dto: GeoCoordinateDto): GeographicCoordinate {
        return GeographicCoordinate(dto.latitude, dto.longitude, dto.altitude)
    }

    private fun filterLegalEntityDuplicatesByIdentifier(
        requests: Collection<LegalEntityPartnerCreateRequest>, errors: MutableList<ErrorInfo<LegalEntityCreateError>>
    ): Collection<LegalEntityPartnerCreateRequest> {

        val idValues = requests.flatMap { it.legalEntity.identifiers }.map { it.value }
        val idsInDb = legalEntityIdentifierRepository.findByValueIn(idValues).map { Pair(it.value, it.type.technicalKey) }.toHashSet()

        val (invalidRequests, validRequests) = requests.partition {
            it.legalEntity.identifiers.map { id -> Pair(id.value, id.type) }.any { id -> idsInDb.contains(id) }
        }

        invalidRequests.map {
            ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "Legal entity not created: duplicate identifier", it.index)
        }.forEach(errors::add)

        return validRequests
    }

    private fun createCurrentnessTimestamp(): Instant {
        return Instant.now().truncatedTo(ChronoUnit.MICROS)
    }
}