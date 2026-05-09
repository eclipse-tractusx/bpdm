/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.dto.*
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

        val (legalEntityMetadataMap, addressMetadataMap) = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        val requestsByLegalEntities = validRequests
            .mapIndexed { bpnIndex, request ->
                val legalEntity = createLegalEntityHeader(request.legalEntity.header, bpnLs[bpnIndex], legalEntityMetadataMap, request.legalEntity.scriptVariants)
                val legalAddress = createLogisticAddress(request.legalEntity.toLegalAddressWithScriptVariants(), bpnAs[bpnIndex], legalEntity, null, addressMetadataMap)
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
            if (legalEntitiesByBpn[siteRequest.bpnLParent] == null) {
                return SitePartnerCreateResponseWrapper(emptyList(), listOf(
                    ErrorInfo(
                        SiteCreateError.LegalEntityNotFound,
                        "Parent ${siteRequest.bpnLParent} not found for site to create",
                        siteRequest.bpnLParent
                    )
                ))
            } else if (legalEntitiesByBpn[siteRequest.bpnLParent]!!.legalAddress.site != null) {
                return SitePartnerCreateResponseWrapper(emptyList(), listOf(
                    ErrorInfo(
                        SiteCreateError.MainAddressDuplicateIdentifier,
                        "Can't create site for legal entity ${siteRequest.bpnLParent} with legal address as site main address: Legal address already belongs to site ${legalEntitiesByBpn[siteRequest.bpnLParent]!!.legalAddress.site!!.bpn}",
                        siteRequest.name
                    )
                ))
            }

            createSite(siteRequest, bpnS, legalEntitiesByBpn[siteRequest.bpnLParent]!!)
                .apply { mainAddress = legalEntitiesByBpn[siteRequest.bpnLParent]!!.legalAddress }
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
    fun createSiteMainAddressFromAdditionalAddress(
        requests: Collection<SitePartnerCreateRequest>,
        address: LogisticAddressDb
    ): SitePartnerCreateResponseWrapper {
        val errorsByRequest = requestValidationService.validateSitesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val (siteHeaderMetadata, mainAddressMetadata) = metadataService.getMetadata(validRequests.map { it.site }).toMapping()

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(validRequests.map { it.bpnlParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }
        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        fun createSiteWithMainAddress(bpnIndex: Int, request: SitePartnerCreateRequest) =
            createSiteHeader(request.site, bpnSs[bpnIndex], legalEntitiesByBpn[request.bpnlParent]!!, siteHeaderMetadata)
                .apply {
                    mainAddress = createLogisticAddress(address, request.site.toMainAddressWithScriptVariants(), address.bpn, this.legalEntity, this, mainAddressMetadata)
                }.let { site -> Pair(site, request) }
        val requestsBySites = validRequests
            .mapIndexed { i, request -> createSiteWithMainAddress(i, request) }
            .toMap()
        val siteResponse = createChangeLogAndSaveSiteInformation(requestsBySites).map { it.toUpsertDto(requestsBySites[it]!!.index) }
        return SitePartnerCreateResponseWrapper(siteResponse, errors)
    }

    @Transactional
    fun createSitesWithMainAddress(requests: Collection<SitePartnerCreateRequest>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites" }

        val errorsByRequest = requestValidationService.validateSitesToCreateFromController(requests)
        val errors = errorsByRequest.flatMap { it.value }
        val validRequests = requests.filterNot { errorsByRequest.containsKey(it) }

        val (siteHeaderMetadata, mainAddressMetadata) = metadataService.getMetadata(validRequests.map { it.site }).toMapping()

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(validRequests.map { it.bpnlParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(validRequests.size)
        val bpnAs = bpnIssuingService.issueAddressBpns(validRequests.size)

        fun createSiteWithMainAddress(bpnIndex: Int, request: SitePartnerCreateRequest) =
            createSiteHeader(request.site, bpnSs[bpnIndex], legalEntitiesByBpn[request.bpnlParent]!!, siteHeaderMetadata)
                .apply { mainAddress = createLogisticAddress(request.site.toMainAddressWithScriptVariants(), bpnAs[bpnIndex], this.legalEntity, this, mainAddressMetadata) }
                .let { site -> Pair(site, request) }

        val requestsBySites = validRequests
            .mapIndexed { i, request -> createSiteWithMainAddress(i, request) }
            .toMap()

        val siteResponse = createChangeLogAndSaveSiteInformation(requestsBySites).map { it.toUpsertDto(requestsBySites[it]!!.index) }

        return SitePartnerCreateResponseWrapper(siteResponse, errors)
    }

    private fun createChangeLogAndSaveSiteInformation(requestsBySites: Map<SiteDb, SitePartnerCreateRequest>): Set<SiteDb> {
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
        return sites
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

        val metadataMap = metadataService.getMetadata(validRequests.map { LogisticAddressWithScriptVariantsDto(it.address, it.scriptVariants) }).toMapping()

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

        val (legalEntityMetadataMap, addressMetadataMap) = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpnl }
        val legalEntities = legalEntityRepository.findDistinctByBpnIn(bpnsToFetch)
        businessPartnerFetchService.fetchDependenciesWithLegalAddress(legalEntities)
        val requestsByBpn = validRequests.associateBy { it.bpnl }

        val legalEntityRequestPairs = legalEntities.map { legalEntity -> Pair(legalEntity, requestsByBpn[legalEntity.bpn]!!) }
        legalEntityRequestPairs.forEach { (legalEntity, request) ->
            val legalEntityBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(legalEntity)
            updateLegalEntity(legalEntity, request.legalEntity.header, legalEntityMetadataMap, request.legalEntity.scriptVariants)
            updateLogisticAddress(legalEntity.legalAddress, request.legalEntity.toLegalAddressWithScriptVariants(), addressMetadataMap)
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

        val (siteHeaderMetadata, mainAddressMetadata) = metadataService.getMetadata(requests.map { it.site }).toMapping()

        val bpnsToFetch = validRequests.map { it.bpns }
        val sites = siteRepository.findDistinctByBpnIn(bpnsToFetch)
        val requestByBpnMap = validRequests.associateBy { it.bpns }

        val siteRequestPairs = sites.map { site -> Pair(site, requestByBpnMap[site.bpn]!!) }
        siteRequestPairs.forEach { (site, request) ->
            val siteBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(site)
            updateSiteHeader(site, request.site, siteHeaderMetadata)
            updateLogisticAddress(site.mainAddress, request.site.toMainAddressWithScriptVariants(), mainAddressMetadata)
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
        val metadataMap = metadataService.getMetadata(validRequests.map { LogisticAddressWithScriptVariantsDto(it.address, it.scriptVariants) }).toMapping()

        val addressRequestPairs = addresses.sortedBy { it.bpn }.zip(requests.sortedBy { it.bpna })
        addressRequestPairs.forEach { (address, request) ->
            val addressBeforeUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)
            updateLogisticAddress(address, request.toAddressWithScriptVariants(), metadataMap)
            val addressAfterUpdate = businessPartnerEquivalenceMapper.toEquivalenceDto(address)

            if (addressBeforeUpdate != addressAfterUpdate) {
                logger.info { "Address ${address.bpn} was updated" }

                logisticAddressRepository.save(address)

                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
            }
        }

        val addressResponses = addresses.map { it.toUpdateDto() }

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
                val address = createLogisticAddress(request.toAddressWithScriptVariants(), bpnAs[i], legalEntity, null, metadataMap)
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
                val address = createLogisticAddress(request.toAddressWithScriptVariants(), bpnAs[i], site.legalEntity, site, metadataMap)
                Pair(address, request.index)
            }

        logisticAddressRepository.saveAll(addressesWithIndex.map { (address, _) -> address })
        return addressesWithIndex.map { (address, index) ->
            address.toCreateResponse(index).also { logger.info { "Address ${address.bpn} was created" } }
        }
    }

    private fun createSiteHeader(
        siteDto: SiteDto,
        bpnS: String,
        partner: LegalEntityDb,
        metadataMap: SiteHeaderMetadataMapping
    ): SiteDb{
        val createdSite = createSite(siteDto, bpnS, partner)
        createdSite.scriptVariants.replace(siteDto.scriptVariants.map { SiteScriptVariantDb(metadataMap.scriptCodes[it.scriptCode]!!, it.name) })

        return createdSite
    }
    private fun updateSiteHeader(site: SiteDb, siteDto: SiteDto, metadataMap: SiteHeaderMetadataMapping): SiteDb {
        updateSite(site, siteDto)
        site.scriptVariants.replace(siteDto.scriptVariants.map { SiteScriptVariantDb(metadataMap.scriptCodes[it.scriptCode]!!, it.name) })

        return site
    }



    private fun createLogisticAddress(
        dto: LogisticAddressWithScriptVariantsDto,
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
        dto: LogisticAddressWithScriptVariantsDto,
        bpn: String,
        metadataMap: AddressMetadataMapping
    ): LogisticAddressDb {
        val address = LogisticAddressDb(
            bpn = bpn,
            legalEntity = null,
            site = null,
            physicalPostalAddress = createPhysicalAddress(dto.address.physicalPostalAddress, metadataMap.regions),
            alternativePostalAddress = dto.address.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) },
            name = dto.address.name,
            confidenceCriteria = createConfidenceCriteria(dto.address.confidenceCriteria)
        )

        updateLogisticAddress(address, dto, metadataMap)

        return address
    }

    private fun createLogisticAddress(
        address: LogisticAddressDb,
        dto: LogisticAddressWithScriptVariantsDto,
        bpn: String,
        legalEntity: LegalEntityDb,
        site: SiteDb?,
        metadataMap: AddressMetadataMapping
    ) = updateLogisticAddressInternal(address, dto, bpn, metadataMap)
        .apply {
            this.legalEntity = legalEntity
            this.site = site
        }

    private fun updateLogisticAddressInternal(
        address: LogisticAddressDb,
        dto: LogisticAddressWithScriptVariantsDto,
        bpn: String,
        metadataMap: AddressMetadataMapping
    ): LogisticAddressDb {
        address.bpn = bpn
        address.legalEntity = null
        address.site = null
        address.physicalPostalAddress = createPhysicalAddress(dto.address.physicalPostalAddress, metadataMap.regions)
        address.alternativePostalAddress = dto.address.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) }
        address.name = dto.address.name
        address.confidenceCriteria = updateConfidenceCriteria(address.confidenceCriteria, dto.address.confidenceCriteria)
        updateLogisticAddress(address, dto, metadataMap)
        return address
    }

    private fun updateLogisticAddress(
        address: LogisticAddressDb,
        dto: LogisticAddressWithScriptVariantsDto,
        metadataMap: AddressMetadataMapping
    ) {
        val addressDto = dto.address

        address.name = addressDto.name
        address.physicalPostalAddress = createPhysicalAddress(addressDto.physicalPostalAddress, metadataMap.regions)
        address.alternativePostalAddress = addressDto.alternativePostalAddress?.let { createAlternativeAddress(it, metadataMap.regions) }

        address.identifiers.apply {
            clear()
            addAll(addressDto.identifiers.map { toAddressIdentifier(it, metadataMap.idTypes, address) })
        }
        address.states.apply {
            clear()
            addAll(addressDto.states.map { toAddressState(it, address) })
        }

        address.confidenceCriteria = updateConfidenceCriteria(address.confidenceCriteria, addressDto.confidenceCriteria)

        val scriptVariants = dto.scriptVariants.map { toAddressScriptVariantDb(metadataMap.scriptCodes[it.scriptCode]!!, it) }
        address.scriptVariants.replace(scriptVariants)
    }

    private fun LegalEntityHeaderMetadataDto.toMapping() =
        LegalEntityHeaderMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            legalForms = legalForms.associateBy { it.technicalKey },
            scriptCodes = scriptCodes.associateBy { it.technicalKey }
        )

    private fun SiteMetadataDto.toMapping() =
        Pair(SiteHeaderMetadataMapping(scriptCodes.associateBy { it.technicalKey }), addressMetadata.toMapping())

    private fun AddressMetadataDto.toMapping() =
        AddressMetadataMapping(
            idTypes = idTypes.associateBy { it.technicalKey },
            regions = regions.associateBy { it.regionCode },
            scriptCodes = scriptCodes.associateBy { it.technicalKey }
        )

    private fun AddressInvariantMetadataDto.toMapping() =
        AddressMetadataDto(idTypes, regions, emptyList()).toMapping()

    private fun LegalEntityMetadataDto.toMapping() =
        Pair(
            headerMetadata.toMapping(),
            legalAddressMetadata.toMapping()
        )

    data class LegalEntityHeaderMetadataMapping(
        val idTypes: Map<String, IdentifierTypeDb>,
        val legalForms: Map<String, LegalFormDb>,
        val scriptCodes: Map<String, ScriptCodeDb>
    )

    data class SiteHeaderMetadataMapping(
        val scriptCodes: Map<String, ScriptCodeDb>
    )

    data class AddressMetadataMapping(
        val idTypes: Map<String, IdentifierTypeDb>,
        val regions: Map<String, RegionDb>,
        val scriptCodes: Map<String, ScriptCodeDb>
    )

    private fun toAddressScriptVariantDb(scriptCode: ScriptCodeDb, scriptVariant: LogisticAddressScriptVariantDto): LogisticAddressScriptVariantDb{
        return LogisticAddressScriptVariantDb(
            scriptCode = scriptCode,
            name = scriptVariant.address.addressName,
            physicalAddress = toPhysicalAddressScriptVariantDb(scriptVariant.address.physicalAddress),
            alternativeAddress = scriptVariant.address.alternativeAddress?.let { toAlternativeAddressScriptVariantDb(it) }
        )
    }

    private fun toPhysicalAddressScriptVariantDb(scriptVariant: PhysicalAddressScriptVariantDto): PhysicalAddressScriptVariantDb{
        return with(scriptVariant){
            PhysicalAddressScriptVariantDb(
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let {createStreet(it) },
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }
    }

    private fun toAlternativeAddressScriptVariantDb(alternativeAddressScriptVariant: AlternativeAddressScriptVariantDto): AlternativeAddressScriptVariantDb{
        return with(alternativeAddressScriptVariant){
            AlternativeAddressScriptVariantDb(
                postalCode = postalCode,
                city = city,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }
    }

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

            site.confidenceCriteria = updateConfidenceCriteria(site.confidenceCriteria, siteDto.confidenceCriteria!!)
        }

        fun createSite(
            siteDto: IBaseSiteDto,
            bpnS: String,
            partner: LegalEntityDb
        ): SiteDb {

            val name = siteDto.name ?: throw BpdmValidationException(TaskStepBuildService.CleaningError.SITE_NAME_IS_NULL.message)

            val site = SiteDb(bpn = bpnS, name = name, legalEntity = partner, confidenceCriteria = createConfidenceCriteria(siteDto.confidenceCriteria!!, 1))

            site.states.addAll(siteDto.states
                .map { toSiteState(it, site) })

            return site
        }

        fun createLegalEntityHeader(
            legalEntityHeaderDto: LegalEntityHeaderDto,
            bpnL: String,
            metadataMap: LegalEntityHeaderMetadataMapping,
            scriptVariants: List<LegalEntityScriptVariantDto>
        ): LegalEntityDb {
            // it has to be validated that the legalForm exits
            val legalForm = legalEntityHeaderDto.legalForm?.let { metadataMap.legalForms[it]!! }
            val legalName = NameDb(value = legalEntityHeaderDto.legalName, shortName = legalEntityHeaderDto.legalShortName)
            val newLegalEntity = LegalEntityDb(
                bpn = bpnL,
                legalName = legalName,
                legalForm = legalForm,
                currentness = Instant.now().truncatedTo(ChronoUnit.MICROS),
                confidenceCriteria = createConfidenceCriteria(legalEntityHeaderDto.confidenceCriteria),
                isCatenaXMemberData = legalEntityHeaderDto.isParticipantData
            )
            updateLegalEntity(newLegalEntity, legalEntityHeaderDto, metadataMap, scriptVariants)

            return newLegalEntity
        }
        fun updateLegalEntity(
            legalEntity: LegalEntityDb,
            legalEntityHeaderDto: LegalEntityHeaderDto,
            metadataMap: LegalEntityHeaderMetadataMapping,
            scriptVariants: List<LegalEntityScriptVariantDto>
        ) {
            legalEntity.currentness = createCurrentnessTimestamp()

            legalEntity.legalName = NameDb(value = legalEntityHeaderDto.legalName, shortName = legalEntityHeaderDto.legalShortName)

            legalEntity.legalForm = legalEntityHeaderDto.legalForm?.let { metadataMap.legalForms[it]!! }

            legalEntity.identifiers.replace(legalEntityHeaderDto.identifiers.map { toLegalEntityIdentifier(it, metadataMap.idTypes, legalEntity) })
            legalEntity.states.replace(legalEntityHeaderDto.states.map { toLegalEntityState(it, legalEntity) })
            legalEntity.confidenceCriteria = updateConfidenceCriteria( legalEntity.confidenceCriteria, legalEntityHeaderDto.confidenceCriteria)
            legalEntity.isCatenaXMemberData = legalEntityHeaderDto.isParticipantData

            legalEntity.scriptVariants.replace(scriptVariants.map { variant -> LegalEntityScriptVariantDb(metadataMap.scriptCodes[variant.scriptCode]!!, variant.legalName, variant.shortName) })
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
                street = physicalAddress.street?.let { createStreet(it) },
                companyPostCode = physicalAddress.companyPostalCode,
                industrialZone = physicalAddress.industrialZone,
                building = physicalAddress.building,
                floor = physicalAddress.floor,
                door = physicalAddress.door,
                taxJurisdictionCode = physicalAddress.taxJurisdictionCode
            )
        }

        fun createStreet(street: IBaseStreetDto): StreetDb{
            return with(street){
                StreetDb(
                    name = name,
                    houseNumber = houseNumber,
                    houseNumberSupplement = houseNumberSupplement,
                    milestone = milestone,
                    direction = direction,
                    namePrefix = namePrefix,
                    additionalNamePrefix = additionalNamePrefix,
                    nameSuffix = nameSuffix,
                    additionalNameSuffix = additionalNameSuffix
                )
            }
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

        fun createConfidenceCriteria(confidenceCriteria: IConfidenceCriteriaDto, numberOfSharingMembers: Int = 0) =
            ConfidenceCriteriaDb(
                sharedByOwner = confidenceCriteria.sharedByOwner!!,
                checkedByExternalDataSource = confidenceCriteria.checkedByExternalDataSource!!,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = confidenceCriteria.lastConfidenceCheckAt!!,
                nextConfidenceCheckAt = confidenceCriteria.nextConfidenceCheckAt!!
            )

        fun updateConfidenceCriteria(oldConfidence: ConfidenceCriteriaDb, newConfidence: IConfidenceCriteriaDto) =
            createConfidenceCriteria(newConfidence).copy(numberOfSharingMembers = oldConfidence.numberOfSharingMembers)
    }


    private fun LegalEntityDto.toLegalAddressWithScriptVariants(): LogisticAddressWithScriptVariantsDto{
        return LogisticAddressWithScriptVariantsDto(
            legalAddress,
            scriptVariants.map { it.toAddressVariant() }
        )
    }

    private fun SiteDto.toMainAddressWithScriptVariants(): LogisticAddressWithScriptVariantsDto{
        return LogisticAddressWithScriptVariantsDto(
            mainAddress,
            scriptVariants.map { it.toAddressVariant() }
        )
    }

    private fun AddressPartnerCreateRequest.toAddressWithScriptVariants(): LogisticAddressWithScriptVariantsDto{
        return LogisticAddressWithScriptVariantsDto(
            address,
            scriptVariants
        )
    }

    private fun AddressPartnerUpdateRequest.toAddressWithScriptVariants(): LogisticAddressWithScriptVariantsDto{
        return LogisticAddressWithScriptVariantsDto(
            address,
            scriptVariants
        )
    }

    private fun LegalEntityScriptVariantDto.toAddressVariant() =
        LogisticAddressScriptVariantDto(scriptCode, legalAddress)

    private fun SiteScriptVariantDto.toAddressVariant() =
        LogisticAddressScriptVariantDto(scriptCode, mainAddress)

}