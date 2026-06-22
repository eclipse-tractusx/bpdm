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
import org.eclipse.tractusx.bpdm.pool.mapper.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.LogisticAddressDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.SiteDtoRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.SiteParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
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
    private val businessPartnerEquivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val additionalAddressCreateService: AdditionalAddressCreateService,
    private val additionalAddressUpdateService: AdditionalAddressUpdateService,
    private val addressCreateService: AddressCreateService,
    private val logisticAddressDtoRequestMapper: LogisticAddressDtoRequestMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper,
    private val siteCreateService: SiteCreateService,
    private val siteUpdateService: SiteUpdateService,
    private val siteDtoRequestMapper: SiteDtoRequestMapper,
    private val siteParseErrorMapper: SiteParseErrorMapper
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Create new business partner records from [requests]
     */
    @Transactional
    fun createLegalEntities(requests: Collection<LegalEntityPartnerCreateRequest>): LegalEntityPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new legal entities" }

        val requestList = requests.toList()
        val headerErrorsByRequest = requestValidationService.validateLegalEntitiesToCreateFromController(requests)
        val (legalEntityMetadataMap, _) = metadataService.getMetadata(requests.map { it.legalEntity }).toMapping()

        // The legal address content is validated by the address create service's `parse`, independently of header
        // validation, so a request can report both a header error and an address error (matching the previous behavior).
        val contentResults = addressCreateService.parseContent(
            requestList.map { logisticAddressDtoRequestMapper.toContentRequest(it.legalEntity.toLegalAddressWithScriptVariants()) }
        )

        val errors = mutableListOf<ErrorInfo<LegalEntityCreateError>>()
        val buildable = mutableListOf<Pair<LegalEntityPartnerCreateRequest, AddressContentParsed>>()
        requestList.forEachIndexed { index, request ->
            val headerErrors = headerErrorsByRequest[request].orEmpty()
            errors.addAll(headerErrors)
            when (val contentResult = contentResults[index]) {
                is ParseResult.Failure -> errors.addAll(contentResult.errors.map { addressParseErrorMapper.toLegalEntityCreateErrorInfo(it, request.index) })
                is ParseResult.Success -> if (headerErrors.isEmpty()) buildable.add(request to contentResult.parsed)
            }
        }

        val bpnLs = bpnIssuingService.issueLegalEntityBpns(buildable.size)
        val legalEntitiesByRequest = buildable.mapIndexed { index, (request, content) ->
            val legalEntity = createLegalEntityHeader(request.legalEntity.header, bpnLs[index], legalEntityMetadataMap, request.legalEntity.scriptVariants)
            Triple(legalEntity, request, content)
        }

        val legalEntities = legalEntitiesByRequest.map { it.first }
        // Emit the legal entity changelog before the address create service emits the ADDRESS CREATE changelog, so the
        // overall changelog order stays "legal entity, then its legal address".
        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY)
        })

        // The address create service owns the address BPN + ADDRESS CREATE changelog; the parent here is still unsaved
        // (flushed in the right order at commit thanks to the nullable back-FK and order_inserts).
        val legalAddresses = addressCreateService.create(legalEntitiesByRequest.map { (legalEntity, _, content) ->
            AddressCreateParsed(legalEntity, site = null, content.address, content.scriptVariants)
        })
        legalEntitiesByRequest.zip(legalAddresses).forEach { (entry, address) -> entry.first.legalAddress = address }

        legalEntities.forEach { logger.info { "Legal Entity ${it.bpn} was created" } }
        legalEntityRepository.saveAll(legalEntities)

        val legalEntityResponse = legalEntitiesByRequest.map { (legalEntity, request, _) -> legalEntity.toUpsertDto(request.index) }

        return LegalEntityPartnerCreateResponseWrapper(legalEntityResponse, errors)
    }

    fun createSitesWithLegalAddressAsMain(requests: Collection<SiteCreateRequestWithLegalAddressAsMain>): SitePartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new sites with legal address as site main address" }

        val legalEntities = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnLParent })
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }

        val bpnSs = bpnIssuingService.issueSiteBpns(requests.size)

        val siteHeaderMetadataMapping = SiteHeaderMetadataMapping(metadataService.getSiteHeaderScriptCodes(requests.flatMap { it.scriptVariants })
            .associateBy { it.technicalKey })

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

            createSiteHeader(siteRequest.toHeader(), bpnS, legalEntitiesByBpn[siteRequest.bpnLParent]!!, siteHeaderMetadataMapping)
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
            createSiteHeader(request.site.toHeader(), bpnSs[bpnIndex], legalEntitiesByBpn[request.bpnlParent]!!, siteHeaderMetadata)
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

        val requestList = requests.toList()
        val createRequests = requestList.map { siteDtoRequestMapper.toCreateRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteCreateError>>()
        requestList.zip(siteCreateService.parseAndCreate(createRequests)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toUpsertDto(request.index))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return SitePartnerCreateResponseWrapper(responses, errors)
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

    /**
     * `@Transactional` so parent resolution and [AddressCreateService.parseAndCreate] (which resolves entities, then
     * persists) share one persistence context. The single `bpnParent` is resolved into the explicit (legalEntity, site)
     * parents the service expects (a site parent contributes its own legal entity); that resolution also reports the
     * precise `BpnNotValid`/`SiteNotFound`/`LegalEntityNotFound` parent errors, which `parse` alone could not
     * distinguish. All address-content validation is delegated to `parse`.
     */
    @Transactional
    fun createAddresses(requests: Collection<AddressPartnerCreateRequest>): AddressPartnerCreateResponseWrapper {
        logger.info { "Create ${requests.size} new addresses" }

        val requestList = requests.toList()
        val parents = resolveCreateParents(requestList)

        // Only requests with a resolved parent reach the service; the others already have a parent error.
        val parentErrors = mutableListOf<ErrorInfo<AddressCreateError>>()
        val createRequests = mutableListOf<Pair<AddressPartnerCreateRequest, AddressCreateRequest>>()
        requestList.zip(parents).forEach { (request, parent) ->
            when (parent) {
                is CreateParent.Invalid -> parentErrors.add(parent.error)
                is CreateParent.Resolved -> {
                    val content = logisticAddressDtoRequestMapper.toContentRequest(request.address, request.scriptVariants)
                    createRequests.add(request to AddressCreateRequest(parent.legalEntityBpn, parent.siteBpn, content))
                }
            }
        }

        val responses = mutableListOf<AddressPartnerCreateVerboseDto>()
        val parseErrors = mutableListOf<ErrorInfo<AddressCreateError>>()
        createRequests.zip(additionalAddressCreateService.parseAndCreate(createRequests.map { it.second })).forEach { (pair, result) ->
            val request = pair.first
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.toCreateResponse(request.index))
                is ParseResult.Failure -> parseErrors.addAll(result.errors.map { addressParseErrorMapper.toCreateErrorInfo(it, request.index) })
            }
        }

        return AddressPartnerCreateResponseWrapper(responses, parentErrors + parseErrors)
    }

    private sealed interface CreateParent {
        data class Resolved(val legalEntityBpn: String, val siteBpn: String?) : CreateParent
        data class Invalid(val error: ErrorInfo<AddressCreateError>) : CreateParent
    }

    /**
     * Resolves each request's single `bpnParent` into the explicit (legalEntity, site) pair the create service needs,
     * validating existence in the same pass. A legal-entity parent resolves to itself; a site parent contributes its own
     * legal entity. Positional: result[i] corresponds to requests[i].
     */
    private fun resolveCreateParents(requests: List<AddressPartnerCreateRequest>): List<CreateParent> {
        val typeByBpn = requests.map { it.bpnParent }.associateWith { bpnIssuingService.translateToBusinessPartnerType(it) }
        val legalEntityParentBpns = typeByBpn.filterValues { it == BusinessPartnerType.LEGAL_ENTITY }.keys
        val siteParentBpns = typeByBpn.filterValues { it == BusinessPartnerType.SITE }.keys
        val existingLegalEntityBpns = legalEntityRepository.findDistinctByBpnIn(legalEntityParentBpns).map { it.bpn }.toSet()
        val sitesByBpn = siteRepository.findDistinctByBpnIn(siteParentBpns).associateBy { it.bpn }

        return requests.map { request ->
            val parent = request.bpnParent
            when (typeByBpn[parent]) {
                BusinessPartnerType.LEGAL_ENTITY ->
                    if (parent in existingLegalEntityBpns) CreateParent.Resolved(parent, siteBpn = null)
                    else CreateParent.Invalid(ErrorInfo(AddressCreateError.LegalEntityNotFound, "Parent with BPN '$parent' not found", request.index))
                BusinessPartnerType.SITE ->
                    sitesByBpn[parent]?.let { CreateParent.Resolved(it.legalEntity.bpn, siteBpn = parent) }
                        ?: CreateParent.Invalid(ErrorInfo(AddressCreateError.SiteNotFound, "Parent with BPN '$parent' not found", request.index))
                else -> CreateParent.Invalid(ErrorInfo(AddressCreateError.BpnNotValid, "Parent '$parent' is not a valid BPNL/BPNS", request.index))
            }
        }
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

        val requestList = requests.toList()
        val updateRequests = requestList.map { siteDtoRequestMapper.toUpdateRequest(it) }

        val responses = mutableListOf<SitePartnerCreateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<SiteUpdateError>>()
        requestList.zip(siteUpdateService.parseAndUpdate(updateRequests)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpsertDto(request.bpns))
                is ParseResult.Failure -> errors.addAll(result.errors.map { siteParseErrorMapper.toUpdateErrorInfo(it, request.bpns) })
            }
        }

        return SitePartnerUpdateResponseWrapper(responses, errors)
    }

    /**
     * `@Transactional` so [AddressUpdateService.parseAndUpdate] resolves the target entities and mutates their lazy
     * collections in one persistence context instead of relying on Open-Session-in-View. All validation (including
     * "address not found") is delegated to `parse`; there is no parent to resolve on update.
     */
    @Transactional
    fun updateAddresses(requests: Collection<AddressPartnerUpdateRequest>): AddressPartnerUpdateResponseWrapper {
        logger.info { "Update ${requests.size} business partner addresses" }

        val requestList = requests.toList()
        val updateRequests = requestList.map {
            AddressUpdateRequest(addressBpn = it.bpna, content = logisticAddressDtoRequestMapper.toContentRequest(it.address, it.scriptVariants))
        }

        val responses = mutableListOf<AddressPartnerUpdateVerboseDto>()
        val errors = mutableListOf<ErrorInfo<AddressUpdateError>>()
        requestList.zip(additionalAddressUpdateService.parseAndUpdate(updateRequests)).forEach { (request, result) ->
            when (result) {
                is ParseResult.Success -> responses.add(result.parsed.value.toUpdateDto())
                is ParseResult.Failure -> errors.addAll(result.errors.map { addressParseErrorMapper.toUpdateErrorInfo(it, request.bpna) })
            }
        }

        return AddressPartnerUpdateResponseWrapper(responses, errors)
    }

    private fun createSiteHeader(
        siteHeaderRequest: SiteHeaderDto,
        bpnS: String,
        partner: LegalEntityDb,
        metadataMap: SiteHeaderMetadataMapping
    ): SiteDb{
        val createdSite = createSite(siteHeaderRequest, bpnS, partner)
        createdSite.scriptVariants.replace(siteHeaderRequest.scriptVariants.map { SiteScriptVariantDb(metadataMap.scriptCodes[it.scriptCode]!!, it.name) })

        return createdSite
    }

    // Re-parents an existing address onto a site as its main address (used by createSiteMainAddressFromAdditionalAddress,
    // #3 — still on the legacy builder pending Phase 3).
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

        // Still used by the v6 legacy site mapper (controller/v6/SiteLegacyServiceMapper); the v7 path uses SiteUpdateService.
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

    private fun LegalEntityScriptVariantDto.toAddressVariant() =
        LogisticAddressScriptVariantDto(scriptCode, legalAddress)

    private fun SiteScriptVariantDto.toAddressVariant() =
        LogisticAddressScriptVariantDto(scriptCode, mainAddress)

}