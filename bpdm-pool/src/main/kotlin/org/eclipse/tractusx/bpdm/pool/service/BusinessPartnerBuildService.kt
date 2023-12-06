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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
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


        val errorsByRequest = requestValidationService.validateLegalEntitiesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val legalEntityMetadataMap = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()
        val addressMetadataMap = metadataService.getMetadata(requests.map { it.legalAddress }).toMapping()

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val requestsByLegalEntities = validRequests
            .mapIndexed { bpnIndex, request ->
                val legalEntity = createLegalEntity(request.legalEntity, bpnLs[bpnIndex], request.legalEntity.legalName, legalEntityMetadataMap)
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

        val errorsByRequest = requestValidationService.validateSitesToCreateFromController(requests)
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

        val errorsByRequest = requestValidationService.validateAddressesToCreateFromController(requests)
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

        val errorsByRequest = requestValidationService.validateLegalEntitiesToUpdateFromController(requests)
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
            updateLegalEntity(legalEntity, request.legalEntity, request.legalEntity.legalName, legalEntityMetadataMap)
            updateLogisticAddress(legalEntity.legalAddress, request.legalAddress, addressMetadataMap)
            legalEntityRepository.save(legalEntity)
        }

        val legalEntityResponses = updatedLegalEntities.map { it.toUpsertDto(it.bpn) }

        return LegalEntityPartnerUpdateResponseWrapper(legalEntityResponses, errors)
    }

    @Transactional
    fun updateSites(requests: Collection<SitePartnerUpdateRequest>): SitePartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} sites" }

        val errorsByRequest = requestValidationService.validateSitesToUpdateFromController(requests)
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

        val errorsByRequest = requestValidationService.validateAddressesToUpdateFromController(requests)
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
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap.regions),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) },
            name = dto.name
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddress, dto: LogisticAddressDto, metadataMap: AddressMetadataMapping) {
        address.name = dto.name
        address.physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap.regions)
        address.alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) }

        address.identifiers.apply {
            clear()
            addAll(dto.identifiers.map { toAddressIdentifier(it, metadataMap.idTypes, address) })
        }
        address.states.apply {
            clear()
            addAll(dto.states.map { toAddressState(it, address) })
        }
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


    data class LegalEntityMetadataMapping(
        val idTypes: Map<String, IdentifierType>,
        val legalForms: Map<String, LegalForm>
    )

    data class AddressMetadataMapping(
        val idTypes: Map<String, IdentifierType>,
        val regions: Map<String, Region>
    )

    companion object {

        fun createCurrentnessTimestamp(): Instant {
            return Instant.now().truncatedTo(ChronoUnit.MICROS)
        }

        fun toLegalEntityState(dto: ILegalEntityStateDto, legalEntity: LegalEntity): LegalEntityState {
            return LegalEntityState(
                description = dto.description,
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                legalEntity = legalEntity
            )
        }

        fun toSiteState(dto: ISiteStateDto, site: Site): SiteState {
            return SiteState(
                description = dto.description,
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                site = site
            )
        }

        fun toAddressState(dto: IBaseAddressStateDto, address: LogisticAddress): AddressState {
            return AddressState(
                description = dto.description,
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                address = address
            )
        }

        fun toLegalEntityClassification(dto: IBaseClassificationDto, partner: LegalEntity): LegalEntityClassification {

            val dtoType = dto.type ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.CLASSIFICATION_TYPE_IS_NULL.message)

            return LegalEntityClassification(
                value = dto.value,
                code = dto.code,
                type = dtoType,
                legalEntity = partner
            )
        }

        fun toLegalEntityIdentifier(
            dto: ILegalEntityIdentifierDto,
            idTypes: Map<String, IdentifierType>,
            partner: LegalEntity
        ): LegalEntityIdentifier {
            return LegalEntityIdentifier(
                value = dto.value,
                type = idTypes[dto.type]!!,
                issuingBody = dto.issuingBody,
                legalEntity = partner
            )
        }

        fun toAddressIdentifier(
            dto: IBaseAddressIdentifierDto,
            idTypes: Map<String, IdentifierType>,
            partner: LogisticAddress
        ): AddressIdentifier {
            return AddressIdentifier(
                value = dto.value,
                type = idTypes[dto.type]!!,
                address = partner
            )
        }

        fun updateSite(site: Site, siteDto: IBaseSiteDto) {

            val name = siteDto.name ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.SITE_NAME_IS_NULL.message)

            site.name = name

            site.states.clear()
            site.states.addAll(siteDto.states
                .map { toSiteState(it, site) })
        }

        fun createSite(
            siteDto: IBaseSiteDto,
            bpnS: String,
            partner: LegalEntity
        ): Site {

            val name = siteDto.name ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.SITE_NAME_IS_NULL.message)

            val site = Site( bpn = bpnS, name = name,  legalEntity = partner)

            site.states.addAll(siteDto.states
                .map { toSiteState(it, site) })

            return site
        }

        fun createLegalEntity(
            legalEntityDto: IBaseLegalEntityDto,
            bpnL: String,
            legalNameValue: String?,
            metadataMap: LegalEntityMetadataMapping
        ): LegalEntity {

            if (legalNameValue == null) {
                throw BpdmValidationException(TaskStepBuildService.CleaningError.LEGAL_NAME_IS_NULL.message)
            }

            // it has to be validated that the legalForm exits
            val legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }
            val legalName = Name(value = legalNameValue, shortName = legalEntityDto.legalShortName)
            val newLegalEntity = LegalEntity(
                bpn = bpnL,
                legalName = legalName,
                legalForm = legalForm,
                currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
            )
            updateLegalEntity(newLegalEntity, legalEntityDto, legalNameValue, metadataMap)

            return newLegalEntity
        }
        fun updateLegalEntity(
            legalEntity: LegalEntity,
            legalEntityDto: IBaseLegalEntityDto,
            legalName: String?,
            metadataMap: LegalEntityMetadataMapping
        ) {
            if(legalName == null) {
                throw BpdmValidationException(TaskStepBuildService.CleaningError.LEGAL_NAME_IS_NULL.message)
            }

            legalEntity.currentness = createCurrentnessTimestamp()

            legalEntity.legalName = Name(value = legalName, shortName = legalEntityDto.legalShortName)

            legalEntity.legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }

            legalEntity.identifiers.replace(legalEntityDto.identifiers.map { toLegalEntityIdentifier(it, metadataMap.idTypes, legalEntity) })
            legalEntity.states.replace(legalEntityDto.states.map { toLegalEntityState(it, legalEntity) })
            legalEntity.classifications.replace( legalEntityDto.classifications.map { toLegalEntityClassification(it, legalEntity) }.toSet()
            )
        }

        fun createPhysicalAddress(physicalAddress: IBasePhysicalPostalAddressDto, regions: Map<String, Region>): PhysicalPostalAddress {

            if (physicalAddress.countryCode() == null || physicalAddress.city == null) {
                throw BpdmValidationException(TaskStepBuildService.CleaningError.COUNTRY_CITY_IS_NULL.message)
            }

            return PhysicalPostalAddress(
                geographicCoordinates = physicalAddress.geographicCoordinates?.let { GeographicCoordinate(it.latitude, it.longitude, it.altitude) },
                country = physicalAddress.countryCode()!!,
                administrativeAreaLevel1 = regions[physicalAddress.administrativeAreaLevel1],
                administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
                postCode = physicalAddress.postalCode,
                city = physicalAddress.city!!,
                districtLevel1 = physicalAddress.district,
                street = physicalAddress.street?.let {
                    Street(
                        name = it.name,
                        houseNumber = it.houseNumber,
                        milestone = it.milestone,
                        direction = it.direction
                    )
                },
                companyPostCode = physicalAddress.companyPostalCode,
                industrialZone = physicalAddress.industrialZone,
                building = physicalAddress.building,
                floor = physicalAddress.floor,
                door = physicalAddress.door
            )
        }

        fun createAlternativeAddress(alternativeAddress: IBaseAlternativePostalAddressDto, regions: Map<String, Region>): AlternativePostalAddress {

            if (alternativeAddress.countryCode() == null || alternativeAddress.city == null ||
                alternativeAddress.deliveryServiceType == null || alternativeAddress.deliveryServiceNumber == null
            ) {

                throw BpdmValidationException(TaskStepBuildService.CleaningError.ALTERNATIVE_ADDRESS_DATA_IS_NULL.message)
            }

            return AlternativePostalAddress(
                geographicCoordinates = alternativeAddress.geographicCoordinates?.let { GeographicCoordinate(it.latitude, it.longitude, it.altitude) },
                country = alternativeAddress.countryCode()!!,
                administrativeAreaLevel1 = regions[alternativeAddress.administrativeAreaLevel1],
                postCode = alternativeAddress.postalCode,
                city = alternativeAddress.city!!,
                deliveryServiceType = alternativeAddress.deliveryServiceType!!,
                deliveryServiceNumber = alternativeAddress.deliveryServiceNumber!!,
                deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
            )
        }
    }



}