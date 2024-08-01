/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.*
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
    private val requestValidationService: RequestValidationService,
    private val businessPartnerEquivalenceMapper: BusinessPartnerEquivalenceMapper
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
                val legalEntity = createLegalEntity(request.legalEntity, bpnLs[bpnIndex], legalEntityMetadataMap)
                val legalAddress = createLogisticAddress(request.legalAddress, bpnAs[bpnIndex], legalEntity, null, addressMetadataMap)
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

        legalEntities.map {

            logger.info { logger.info { "Legal Entity ${it.bpn} was created" } }

        }
        legalEntityRepository.saveAll(legalEntities)

        val legalEntityResponse = legalEntities.map { it.toUpsertDto(requestsByLegalEntities[it]!!.index) }

        return LegalEntityPartnerCreateResponseWrapper(legalEntityResponse, errors)
    }

    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnLParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(requests.size)

        val createdSites = requests.zip(bpnSs).map { (siteRequest, bpnS) ->
            val legalEntityParent =
                legalEntitiesByBpn[siteRequest.bpnLParent] ?: throw BpdmValidationException("Parent ${siteRequest.bpnLParent} not found for site to create")

            if(legalEntityParent.legalAddress.site != null)
                throw BpdmValidationException("Can't create site for legal entity ${siteRequest.bpnLParent} with legal address as site main address: Legal address already belongs to site ${legalEntityParent.legalAddress.site!!.bpn}")

            createSite(siteRequest, bpnS, legalEntityParent)
                .apply { mainAddress = legalEntityParent.legalAddress }
                .apply { mainAddress.site = this }
        }

        siteRepository.saveAll(createdSites)

        changelogService.createChangelogEntries(createdSites.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.SITE)
        })

        val siteResponse = createdSites.mapIndexed { index, site -> site.toUpsertDto(index.toString()) }

        return SitePartnerCreateResponseWrapper(siteResponse, emptyList())

    }

    @Transactional
    fun createSitesWithMainAddress(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
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
                .apply { mainAddress = createLogisticAddress(request.site.mainAddress, bpnAs[bpnIndex], this.legalEntity, this, addressMetadataMap) }
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

        sites.map {

            logger.info { "Site ${it.bpn} was created" }

        }

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
        val requestsByBpn = validRequests.associateBy { it.bpnl }

        val legalEntityRequestPairs = legalEntities.map { legalEntity -> Pair(legalEntity, requestsByBpn[legalEntity.bpn]!!) }
        legalEntityRequestPairs.forEach { (legalEntity, request) ->
            val legalEntityBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(legalEntity)
            updateLegalEntity(legalEntity, request.legalEntity, legalEntityMetadataMap)
            updateLogisticAddress(legalEntity.legalAddress, request.legalAddress, addressMetadataMap)
            val legalEntityAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(legalEntity)

            if (legalEntityBeforeUpdate != legalEntityAfterUpdate) {
                logger.info { "Legal Entity ${legalEntity.bpn} was updated" }

                legalEntityRepository.save(legalEntity)

                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            legalEntity.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.LEGAL_ENTITY
                        )
                    )
                )
                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            legalEntity.legalAddress.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.ADDRESS
                        )
                    )
                )
            }
        }

        val legalEntityResponses = legalEntityRequestPairs.map { (legalEntity, request) -> legalEntity.toUpsertDto(request.bpnl) }

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
        val requestByBpnMap = validRequests.associateBy { it.bpns }

        val siteRequestPairs = sites.map { site -> Pair(site, requestByBpnMap[site.bpn]!!) }
        siteRequestPairs.forEach { (site, request) ->
            val siteBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(site)
            updateSite(site, request.site)
            updateLogisticAddress(site.mainAddress, request.site.mainAddress, addressMetadataMap)
            val siteAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(site)

            if (siteBeforeUpdate != siteAfterUpdate) {
                logger.info { "Site ${site.bpn} was updated" }

                siteRepository.save(site)

                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(site.bpn, ChangelogType.UPDATE, BusinessPartnerType.SITE)))
                changelogService.createChangelogEntries(
                    listOf(
                        ChangelogEntryCreateRequest(
                            site.mainAddress.bpn,
                            ChangelogType.UPDATE,
                            BusinessPartnerType.ADDRESS
                        )
                    )
                )
            }
        }

        val siteResponses = siteRequestPairs.map { (site, request) -> site.toUpsertDto(request.bpns) }

        return SitePartnerUpdateResponseWrapper(siteResponses, errors)
    }

    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val errorsByRequest = requestValidationService.validateAddressesToUpdateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val addresses = logisticAddressRepository.findDistinctByBpnIn(validRequests.map { it.bpna })
        val metadataMap = metadataService.getMetadata(validRequests.map { it.address }).toMapping()

        val addressRequestPairs = addresses.sortedBy { it.bpn }.zip(requests.sortedBy { it.bpna })
        addressRequestPairs.forEach { (address, request) ->
            val addressBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)
            updateLogisticAddress(address, request.address, metadataMap)
            val addressAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)

            if (addressBeforeUpdate != addressAfterUpdate) {
                logger.info { "Address ${address.bpn} was updated" }

                logisticAddressRepository.save(address)

                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            }
        }

        val addressResponses = addresses.map { it.toDto() }

        return AddressPartnerUpdateResponseWrapper(addressResponses, errors)
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
                val address = createLogisticAddress(request.address, bpnAs[i], legalEntity, null, metadataMap)
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
                val address = createLogisticAddress(request.address, bpnAs[i], site.legalEntity, site, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) ->
            address.toCreateResponse(index).also { logger.info { "Address ${address.bpn} was created" } }
        }
    }


    private fun createLogisticAddress(
        dto: LogisticAddressDto,
        bpn: String,
        legalEntity: LegalEntityDb,
        site: SiteDb?,
        metadataMap: AddressMetadataMapping
    ) = createLogisticAddressInternal(dto, bpn, metadataMap)
        .apply {
            this.legalEntity = legalEntity
            this.site = site
        }

    private fun createLogisticAddressInternal(
        dto: LogisticAddressDto,
        bpn: String,
        metadataMap: AddressMetadataMapping
    ): LogisticAddressDb {
        val address = LogisticAddressDb(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.physicalPostalAddress, metadataMap.regions),
            alternativePostalAddress = dto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) },
            name = dto.name,
            confidenceCriteria = createConfidenceCriteria(dto.confidenceCriteria)
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun updateLogisticAddress(address: LogisticAddressDb, dto: LogisticAddressDto, metadataMap: AddressMetadataMapping) {
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

        address.confidenceCriteria = createConfidenceCriteria(dto.confidenceCriteria)
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
        val idTypes: Map<String, IdentifierTypeDb>,
        val legalForms: Map<String, LegalFormDb>
    )

    data class AddressMetadataMapping(
        val idTypes: Map<String, IdentifierTypeDb>,
        val regions: Map<String, RegionDb>
    )

    companion object {

        fun createCurrentnessTimestamp(): Instant {
            return Instant.now().truncatedTo(ChronoUnit.MICROS)
        }

        fun toLegalEntityState(dto: ILegalEntityStateDto, legalEntity: LegalEntityDb): LegalEntityStateDb {
            return LegalEntityStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                legalEntity = legalEntity
            )
        }

        fun toSiteState(dto: ISiteStateDto, site: SiteDb): SiteStateDb {
            return SiteStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                site = site
            )
        }

        fun toAddressState(dto: IAddressStateDto, address: LogisticAddressDb): AddressStateDb {
            return AddressStateDb(
                validFrom = dto.validFrom,
                validTo = dto.validTo,
                type = dto.type,
                address = address
            )
        }

        fun toLegalEntityClassification(dto: IBaseClassificationDto, partner: LegalEntityDb): LegalEntityClassificationDb {

            val dtoType = dto.type ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.CLASSIFICATION_TYPE_IS_NULL.message)

            return LegalEntityClassificationDb(
                value = dto.value,
                code = dto.code,
                type = dtoType,
                legalEntity = partner
            )
        }

        fun toLegalEntityIdentifier(
            dto: ILegalEntityIdentifierDto,
            idTypes: Map<String, IdentifierTypeDb>,
            partner: LegalEntityDb
        ): LegalEntityIdentifierDb {
            return LegalEntityIdentifierDb(
                value = dto.value,
                type = idTypes[dto.type]!!,
                issuingBody = dto.issuingBody,
                legalEntity = partner
            )
        }

        fun toAddressIdentifier(
            dto: IAddressIdentifierDto,
            idTypes: Map<String, IdentifierTypeDb>,
            partner: LogisticAddressDb
        ): AddressIdentifierDb {
            return AddressIdentifierDb(
                value = dto.value,
                type = idTypes[dto.type]!!,
                address = partner
            )
        }

        fun updateSite(site: SiteDb, siteDto: IBaseSiteDto) {

            val name = siteDto.name ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.SITE_NAME_IS_NULL.message)

            site.name = name

            site.states.clear()
            site.states.addAll(siteDto.states.map { toSiteState(it, site) })

            site.confidenceCriteria = createConfidenceCriteria(siteDto.confidenceCriteria!!)
        }

        fun createSite(
            siteDto: IBaseSiteDto,
            bpnS: String,
            partner: LegalEntityDb
        ): SiteDb {

            val name = siteDto.name ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.SITE_NAME_IS_NULL.message)

            val site = SiteDb(bpn = bpnS, name = name, legalEntity = partner, confidenceCriteria = createConfidenceCriteria(siteDto.confidenceCriteria!!))

            site.states.addAll(siteDto.states
                .map { toSiteState(it, site) })

            return site
        }

        fun createLegalEntity(
            legalEntityDto: LegalEntityDto,
            bpnL: String,
            metadataMap: LegalEntityMetadataMapping
        ): LegalEntityDb {
            // it has to be validated that the legalForm exits
            val legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }
            val legalName = NameDb(value = legalEntityDto.legalName, shortName = legalEntityDto.legalShortName)
            val newLegalEntity = LegalEntityDb(
                bpn = bpnL,
                legalName = legalName,
                legalForm = legalForm,
                currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
                confidenceCriteria = createConfidenceCriteria(legalEntityDto.confidenceCriteria),
                isCatenaXMemberData = legalEntityDto.isCatenaXMemberData
            )
            updateLegalEntity(newLegalEntity, legalEntityDto, metadataMap)

            return newLegalEntity
        }
        fun updateLegalEntity(
            legalEntity: LegalEntityDb,
            legalEntityDto: LegalEntityDto,
            metadataMap: LegalEntityMetadataMapping
        ) {
            legalEntity.currentness = createCurrentnessTimestamp()

            legalEntity.legalName = NameDb(value = legalEntityDto.legalName, shortName = legalEntityDto.legalShortName)

            legalEntity.legalForm = legalEntityDto.legalForm?.let { metadataMap.legalForms[it]!! }

            legalEntity.identifiers.replace(legalEntityDto.identifiers.map { toLegalEntityIdentifier(it, metadataMap.idTypes, legalEntity) })
            legalEntity.states.replace(legalEntityDto.states.map { toLegalEntityState(it, legalEntity) })
            legalEntity.confidenceCriteria = createConfidenceCriteria(legalEntityDto.confidenceCriteria)
            legalEntity.isCatenaXMemberData = legalEntityDto.isCatenaXMemberData
        }

        fun createPhysicalAddress(physicalAddress: IBasePhysicalPostalAddressDto, regions: Map<String, RegionDb>): PhysicalPostalAddressDb {

            if (physicalAddress.country == null || physicalAddress.city == null) {
                throw BpdmValidationException(TaskStepBuildService.CleaningError.COUNTRY_CITY_IS_NULL.message)
            }

            return PhysicalPostalAddressDb(
                geographicCoordinates = physicalAddress.geographicCoordinates?.let { GeographicCoordinateDb(it.latitude, it.longitude, it.altitude) },
                country = physicalAddress.country!!,
                administrativeAreaLevel1 = regions[physicalAddress.administrativeAreaLevel1],
                administrativeAreaLevel2 = physicalAddress.administrativeAreaLevel2,
                administrativeAreaLevel3 = physicalAddress.administrativeAreaLevel3,
                postCode = physicalAddress.postalCode,
                city = physicalAddress.city!!,
                districtLevel1 = physicalAddress.district,
                street = physicalAddress.street?.let {
                    StreetDb(
                        name = it.name,
                        houseNumber = it.houseNumber,
                        houseNumberSupplement = it.houseNumberSupplement,
                        milestone = it.milestone,
                        direction = it.direction,
                        namePrefix = it.namePrefix,
                        additionalNamePrefix = it.additionalNamePrefix,
                        nameSuffix = it.nameSuffix,
                        additionalNameSuffix = it.additionalNameSuffix
                    )
                },
                companyPostCode = physicalAddress.companyPostalCode,
                industrialZone = physicalAddress.industrialZone,
                building = physicalAddress.building,
                floor = physicalAddress.floor,
                door = physicalAddress.door,
                taxJurisdictionCode = physicalAddress.taxJurisdictionCode
            )
        }

        fun createAlternativeAddress(alternativeAddress: IBaseAlternativePostalAddressDto, regions: Map<String, RegionDb>): AlternativePostalAddressDb {

            if (alternativeAddress.country == null || alternativeAddress.city == null ||
                alternativeAddress.deliveryServiceType == null || alternativeAddress.deliveryServiceNumber == null
            ) {

                throw BpdmValidationException(TaskStepBuildService.CleaningError.ALTERNATIVE_ADDRESS_DATA_IS_NULL.message)
            }

            return AlternativePostalAddressDb(
                geographicCoordinates = alternativeAddress.geographicCoordinates?.let { GeographicCoordinateDb(it.latitude, it.longitude, it.altitude) },
                country = alternativeAddress.country!!,
                administrativeAreaLevel1 = regions[alternativeAddress.administrativeAreaLevel1],
                postCode = alternativeAddress.postalCode,
                city = alternativeAddress.city!!,
                deliveryServiceType = alternativeAddress.deliveryServiceType!!,
                deliveryServiceNumber = alternativeAddress.deliveryServiceNumber!!,
                deliveryServiceQualifier = alternativeAddress.deliveryServiceQualifier
            )
        }

        fun createConfidenceCriteria(confidenceCriteria: IConfidenceCriteriaDto) =
            ConfidenceCriteriaDb(
                sharedByOwner = confidenceCriteria.sharedByOwner!!,
                checkedByExternalDataSource = confidenceCriteria.checkedByExternalDataSource!!,
                numberOfBusinessPartners = confidenceCriteria.numberOfSharingMembers!!,
                lastConfidenceCheckAt = confidenceCriteria.lastConfidenceCheckAt!!,
                nextConfidenceCheckAt = confidenceCriteria.nextConfidenceCheckAt!!,
                confidenceLevel = confidenceCriteria.confidenceLevel!!
            )
    }

}