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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmMultiValidationException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskAddressRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskLegalEntityRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskSiteRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSiteAssignmentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.*
import org.eclipse.tractusx.bpdm.pool.service.parser.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service


@Service
class TaskStepBuildService(
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val addressService: AddressService,
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    private val taskResolutionMapper: TaskResolutionMapper,
    private val addressResponseMapper: AddressResponseMapper,
    private val siteResponseMapper: SiteResponseMapper,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val siteRepository: SiteRepository,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val typedParentAddressCreateParser: TypedParentAddressCreateParser,
    private val addressCreateService: AddressCreateService,
    private val addressUpdateParser: AddressUpdateParser,
    private val addressPayloadUpdateService: AddressPayloadUpdateService,
    private val taskAddressRequestMapper: GoldenRecordTaskAddressRequestMapper,
    private val legalEntityCreateParser: LegalEntityCreateParser,
    private val legalEntityCreateService: LegalEntityCreateService,
    private val legalEntityUpdateParser: LegalEntityUpdateParser,
    private val legalEntityPayloadUpdateService: LegalEntityPayloadUpdateService,
    private val siteCreateParser: SiteCreateParser,
    private val siteCreateService: SiteCreateService,
    private val siteUpdateParser: SiteUpdateParser,
    private val sitePayloadUpdateService: SitePayloadUpdateService,
    private val siteCreateWithLegalAddressAsMainParser: SiteCreateWithLegalAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainParser: SiteCreateWithReferencedAddressAsMainParser,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val siteCreateOnAddressParser: SiteCreateOnAddressParser,
    private val addressSiteAssignmentParser: AddressSiteAssignmentParser,
    private val addressUpdateService: AddressUpdateService,
    private val taskLegalEntityRequestMapper: GoldenRecordTaskLegalEntityRequestMapper,
    private val taskSiteRequestMapper: GoldenRecordTaskSiteRequestMapper,
    private val coverageValidator: TaskScriptVariantCoverageValidator
) {

    enum class CleaningError(val message: String) {
        LEGAL_NAME_IS_NULL("Legal name is null"),
        COUNTRY_CITY_IS_NULL("Country or city in physicalAddress is null"),
        ALTERNATIVE_ADDRESS_DATA_IS_NULL("Country or city or deliveryServiceType or deliveryServiceNumber in alternativeAddress is null"),
        MAINE_ADDRESS_IS_NULL("Main address is null"),
        BPNA_IS_NULL("BpnA Reference is null"),
        SITE_NAME_IS_NULL("Site name is null"),
        PHYSICAL_ADDRESS_COUNTRY_MISSING("Physical Address has no country"),
        PHYSICAL_ADDRESS_CITY_MISSING("Physical Address has no city"),
        ALTERNATIVE_ADDRESS_COUNTRY_MISSING("Alternative Address has no country"),
        ALTERNATIVE_ADDRESS_CITY_MISSING("Alternative Address has no city"),
        ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING("Alternative Address has no delivery service type"),
        ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING("Alternative Address has no delivery service number"),
        ADDRESS_CONFIDENCE_CRITERIA_MISSING("Logistic address is missing confidence criteria"),
        SITE_CONFIDENCE_CRITERIA_MISSING("Site is missing confidence criteria"),
        SITE_NAME_MISSING("Site has no name"),
        LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING("Legal Entity has no confidence criteria"),
        SITE_WRONG_LEGAL_ENTITY_REFERENCE("The legal entity is not the parent of the site"),
        ADDITIONAL_ADDRESS_WRONG_SITE_REFERENCE("The site is not the parent of the additional address"),
        ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE("The legal entity is not the parent of the additional address")
    }

    @Transactional
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val taskEntryBpnMapping = TaskEntryBpnMapping(listOf(taskEntry), bpnRequestIdentifierRepository)
        val businessPartnerDto = taskEntry.businessPartner

        assertParentsConsistent(businessPartnerDto, taskEntryBpnMapping)
        assertScriptVariantCoverage(businessPartnerDto, taskEntryBpnMapping)

        val legalEntityResult = processLegalEntity(businessPartnerDto, taskEntryBpnMapping)
        val siteResult = processSite(businessPartnerDto, legalEntityResult.bpnReference.referenceValue!!, taskEntryBpnMapping)
        val addressResult = processAdditionalAddress(businessPartnerDto, legalEntityResult.bpnReference.referenceValue!!, siteResult?.bpnReference?.referenceValue, taskEntryBpnMapping)

        // The address the additional sites attach to only exists once its own golden record has been written, so this
        // runs after all three components.
        val recordAddressBpn = recordAddressBpn(businessPartnerDto.type!!, legalEntityResult, siteResult, addressResult)
        processAdditionalSites(businessPartnerDto, recordAddressBpn, taskEntryBpnMapping)
        // Additional sites are the sites of the address next to the site this data is about, so business partner data
        // without a site of its own reports none - the same rule the data has to satisfy on its way in.
        val additionalSiteResults = siteResult
            ?.let { readAdditionalSites(recordAddressBpn, it.bpnReference.referenceValue) }
            .orEmpty()

        val (updatedLegalEntityResult, updatedSiteResult, updatedAddressResult) =
            updateConfidences(businessPartnerDto.type!!, taskEntry.recordId, legalEntityResult, siteResult, addressResult)

        taskEntryBpnMapping.writeCreatedMappingsToDb(bpnRequestIdentifierRepository)

        return buildTaskReply(
            taskEntry.taskId,
            businessPartnerDto,
            updatedLegalEntityResult,
            updatedSiteResult,
            updatedAddressResult,
            additionalSiteResults
        )
    }

    private fun processAdditionalSites(
        businessPartner: BusinessPartner,
        recordAddressBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ) {
        // The same site stated twice is one statement written twice, not two memberships. An entry is identified by the
        // reference it carries and, carrying none, by the name its site is to be created under - as far as identity goes
        // here: resolving a name to an existing site is the refinement service's job, not this one's.
        val statedOnce = businessPartner.additionalSites.distinctBy { it.bpnReference.referenceValue ?: it.siteName }
        val (known, unknown) = statedOnce.partition { taskEntryBpnMapping.getBpn(it.bpnReference) != null }

        createAdditionalSites(unknown, businessPartner, recordAddressBpn, taskEntryBpnMapping)
        linkAdditionalSites(known.map { taskEntryBpnMapping.getBpn(it.bpnReference)!! }.distinct(), recordAddressBpn)
    }

    private fun createAdditionalSites(
        additionalSites: List<AdditionalSite>,
        businessPartner: BusinessPartner,
        recordAddressBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ) {
        if (additionalSites.isEmpty()) return

        val confidenceCriteria = additionalSiteConfidence(businessPartner)
        val requests = additionalSites.map { taskSiteRequestMapper.toCreateOnAddressRequest(recordAddressBpn, it, confidenceCriteria) }

        val createdSites = parseAndExecuteAllOrNone(
            requests,
            siteCreateOnAddressParser::parse,
            { errors -> BpdmMultiValidationException(errors.map { renderError(it) }) },
            siteCreateWithReferencedAddressAsMainService::create
        )

        additionalSites.zip(createdSites).forEach { (additionalSite, createdSite) ->
            taskEntryBpnMapping.addMapping(additionalSite.bpnReference, createdSite.bpn)
        }
    }

    private fun linkAdditionalSites(siteBpns: List<String>, recordAddressBpn: String) {
        if (siteBpns.isEmpty()) return

        val requests = siteBpns.map { AddressSiteAssignmentRequest(addressBpn = recordAddressBpn, siteBpn = it) }

        parseAndExecuteAllOrNone(
            requests,
            addressSiteAssignmentParser::parse,
            { errors -> BpdmMultiValidationException(errors.map { renderError(it) }) },
            addressUpdateService::assignToSites
        )
    }

    /**
     * A site created here has had no confidence assessed for it of its own, so it borrows the assessment the record
     * carries for the business partner it is about.
     */
    private fun additionalSiteConfidence(businessPartner: BusinessPartner): ConfidenceCriteria =
        businessPartner.site?.confidenceCriteria
            ?: businessPartner.additionalAddress?.confidenceCriteria
            ?: businessPartner.legalEntity.legalAddress.confidenceCriteria

    private fun readAdditionalSites(recordAddressBpn: String, recordSiteBpn: String?): List<AdditionalSite> =
        addressService.findAddressByBpn(recordAddressBpn)
            ?.sites
            .orEmpty()
            .filterNot { it.bpn == recordSiteBpn }
            .sortedBy { it.createdAt }
            .map { AdditionalSite(BpnReference(it.bpn, null, BpnReferenceType.Bpn), it.name) }

    private fun recordAddressBpn(
        goldenRecordType: GoldenRecordType,
        legalEntityResult: LegalEntity,
        siteResult: Site?,
        additionalAddressResult: PostalAddressWithScriptVariants?
    ): String =
        when (goldenRecordType) {
            GoldenRecordType.LegalEntity -> legalEntityResult.legalAddress.bpnReference.referenceValue!!
            GoldenRecordType.Site -> siteResult!!.siteMainAddress!!.bpnReference.referenceValue!!
            GoldenRecordType.Address -> additionalAddressResult!!.bpnReference.referenceValue!!
        }

    private fun processLegalEntity(
        businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntity{
        val legalEntity = businessPartner.legalEntity
        val bpnLReference = legalEntity.bpnReference
        val bpnL = taskEntryBpnMapping.getBpn(bpnLReference)

        val existingLegalEntityInformation by lazy { fetchLegalEntityResult(bpnL!!, hasChanged = false) }

        val isDataSpaceParticipant = legalEntity.isParticipantData ?: if(bpnL != null) existingLegalEntityInformation.isParticipantData else false

        val legalEntityResult = if(bpnL != null && legalEntity.hasChanged == false){
            //No need to upsert, just fetch the information
            existingLegalEntityInformation
        }else{
            upsertLegalEntity(legalEntity.copy(isParticipantData = isDataSpaceParticipant), taskEntryBpnMapping)
        }

        return legalEntityResult
    }


    private fun upsertLegalEntity(
        legalEntity: LegalEntity, taskEntryBpnMapping: TaskEntryBpnMapping
    ): LegalEntity {
        val legalAddress = legalEntity.legalAddress
        val bpnLReference = legalEntity.bpnReference
        val bpnL = taskEntryBpnMapping.getBpn(bpnLReference)

        val upsertedLegalEntity = if (bpnL == null) createLegalEntity(legalEntity) else updateLegalEntity(bpnL, legalEntity)

        taskEntryBpnMapping.addMapping(bpnLReference, upsertedLegalEntity.bpn)
        taskEntryBpnMapping.addMapping(legalAddress.bpnReference, upsertedLegalEntity.legalAddress.bpn)

        // Read the upserted golden record back so the reply carries its full state (matches the address path).
        return fetchLegalEntityResult(upsertedLegalEntity.bpn, hasChanged = true)
    }

    private fun createLegalEntity(legalEntity: LegalEntity): LegalEntityDb {
        val request = taskLegalEntityRequestMapper.toCreateRequest(legalEntity)
        return when (val result = parseAndExecute(listOf(request), legalEntityCreateParser::parse, legalEntityCreateService::create).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { renderError(it) })
        }
    }

    private fun updateLegalEntity(bpnL: String, legalEntity: LegalEntity): LegalEntityDb {
        val request = taskLegalEntityRequestMapper.toUpdateRequest(bpnL, legalEntity)
        return when (val result = parseAndExecute(listOf(request), legalEntityUpdateParser::parseWithoutCoverageCheck, legalEntityPayloadUpdateService::update).single()) {
            is ParseResult.Success -> result.parsed.value
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { renderError(it) })
        }
    }

    private fun fetchLegalEntityResult(bpnL: String, hasChanged: Boolean?): LegalEntity =
        businessPartnerFetchService.fetchDtosByBpns(listOf(bpnL)).firstOrNull()
            ?.let { taskResolutionMapper.toTaskResult(it, hasChanged) }
            ?: throw BpdmValidationException("Legal entity with specified BPNL $bpnL not found")

    private fun processSite(
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): Site? {
        val site = businessPartner.site ?: return null

        val bpnSReference = site.bpnReference
        val bpnS = taskEntryBpnMapping.getBpn(bpnSReference)

        val siteResult = if(bpnS != null && site.hasChanged == false){
            //No need to upsert, just fetch the information
            fetchSiteResult(bpnS, hasChanged = false)
        } else {
            val bpnA = taskEntryBpnMapping.getBpn(site.siteMainAddress?.bpnReference)
            if (bpnA == null) {
                upsertSite(site, businessPartner, legalEntityBpn, taskEntryBpnMapping)
            } else {
                updateAddressLinkage(bpnA, site, businessPartner, legalEntityBpn, taskEntryBpnMapping)
            }
        }

        return siteResult
    }

    private fun updateAddressLinkage(
        bpnA: String,
        site: Site,
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): Site {
        val address = addressService.findAddressByBpn(bpnA)
        val bpnS = taskEntryBpnMapping.getBpn(site.bpnReference)
        // A NEW site (no BPN yet) whose main-address reference already resolves to a persisted address adopts
        // that address as its main address - so several sites can share one main address - instead of creating a
        // duplicate. An existing site being updated, and the legal-address-as-main path, stay on upsertSite (the
        // latter already re-parents the legal address via the referenced-address service).
        return if (address != null && bpnS == null && !site.siteMainIsLegalAddress) {
            createSiteOnExistingAddress(site, businessPartner, address, taskEntryBpnMapping)
        } else {
            upsertSite(site, businessPartner, legalEntityBpn, taskEntryBpnMapping)
        }
    }

    private fun createSiteOnExistingAddress(
        site: Site,
        businessPartner: BusinessPartner,
        existingAddress: LogisticAddressDb,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): Site {
        // Reached only via the address-linkage path for a new site, where the site carries its own main address
        // reference resolving to an already-persisted address (an additional address, or another site's main
        // address). The referenced service re-parents that existing address onto the new site - adding the site to
        // the address's site set - and derives the legal-entity parent from the address itself. The task states that
        // address's content too, so it is applied: the site's own view of its main address is its golden record.
        val siteMainAddress = site.siteMainAddress ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)
        val bpnSReference = site.bpnReference
        val mergedSite = site.withRelevantScriptVariants(businessPartner)

        val request = taskSiteRequestMapper.toCreateWithReferencedAddressAsMainRequest(existingAddress.bpn, mergedSite, siteMainAddress)
        val createdSite = when (val result = parseAndExecute(listOf(request), siteCreateWithReferencedAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { renderError(it) })
        }

        taskEntryBpnMapping.addMapping(bpnSReference, createdSite.bpn)
        taskEntryBpnMapping.addMapping(siteMainAddress.bpnReference, createdSite.mainAddress.bpn)
        return fetchSiteResult(createdSite.bpn, hasChanged = true)
    }

    private fun upsertSite(
        site: Site,
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): Site {
        val isSiteMainAndLegalAddress = site.siteMainIsLegalAddress
        val siteMainAddress = if(isSiteMainAndLegalAddress) businessPartner.legalEntity.legalAddress
            else (site.siteMainAddress ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message))

        val bpnSReference = site.bpnReference
        val bpnS = taskEntryBpnMapping.getBpn(bpnSReference)

        val mergedSite = site.withRelevantScriptVariants(businessPartner)

        val upsertedSite = if (bpnS == null) {
            createSite(legalEntityBpn, mergedSite, siteMainAddress, isSiteMainAndLegalAddress)
        }
        else {
            updateSite(bpnS, mergedSite, siteMainAddress, businessPartner.legalAddressCoverageNotStatedBy(mergedSite))
        }

        taskEntryBpnMapping.addMapping(bpnSReference, upsertedSite.bpn)
        if(!isSiteMainAndLegalAddress)
            taskEntryBpnMapping.addMapping(siteMainAddress.bpnReference, upsertedSite.mainAddress.bpn)

        return fetchSiteResult(upsertedSite.bpn, hasChanged = true)
    }

    private fun createSite(
        legalEntityBpn: String,
        site: Site,
        mainAddress: PostalAddress,
        isSiteMainAndLegalAddress: Boolean
    ): SiteDb {
        val result = if(isSiteMainAndLegalAddress){
            val request = taskSiteRequestMapper.toCreateWithLegalAddressAsMainRequest(legalEntityBpn, site)
            parseAndExecute(listOf(request), siteCreateWithLegalAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create).single()
        }else{
            val request = taskSiteRequestMapper.toCreateRequest(legalEntityBpn, site, mainAddress)
            parseAndExecute(listOf(request), siteCreateParser::parse, siteCreateService::create).single()
        }

        return when (result) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { renderError(it) })
        }
    }

    private fun updateSite(
        bpnS: String,
        site: Site,
        mainAddress: PostalAddress,
        additionalMainAddressScriptVariants: List<PostalAddressScriptVariantWithScriptCode>
    ): SiteDb {
        val request = taskSiteRequestMapper.toUpdateRequest(bpnS, site, mainAddress, additionalMainAddressScriptVariants)
        return when (val result = parseAndExecute(listOf(request), siteUpdateParser::parseWithoutCoverageCheck, sitePayloadUpdateService::update).single()) {
            is ParseResult.Success -> result.parsed.value
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { renderError(it) })
        }
    }

    private fun fetchSiteResult(bpnS: String, hasChanged: Boolean?): Site =
        siteRepository.findByBpn(bpnS)?.let { siteResponseMapper.toSiteWithMainAddress(it) }
            ?.let { taskResolutionMapper.toTaskResult(it.site, it.mainAddress, hasChanged) }
            ?: throw BpdmValidationException(CleaningError.MAINE_ADDRESS_IS_NULL.message)

    private fun processAdditionalAddress(
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): PostalAddressWithScriptVariants? {
        val additionalAddress = businessPartner.additionalAddress ?: return null

        val bpnAReference = additionalAddress.bpnReference
        val bpnA = taskEntryBpnMapping.getBpn(bpnAReference)

        return if (bpnA != null && additionalAddress.hasChanged == false) {
            // No need to upsert, just fetch the data
            fetchAddressResult(bpnA, hasChanged = false)
        } else {
            upsertAdditionalAddress(additionalAddress, legalEntityBpn, siteBpn, taskEntryBpnMapping)
        }
    }

    private fun upsertAdditionalAddress(
        additionalAddress: PostalAddressWithScriptVariants,
        legalEntityBpn: String,
        siteBpn: String?,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): PostalAddressWithScriptVariants {
        val bpnAReference = additionalAddress.bpnReference
        val bpnA = taskEntryBpnMapping.getBpn(bpnAReference)

        val upsertedBpn = if (bpnA == null) {
            createLogisticAddress(additionalAddress, legalEntityBpn, siteBpn)
        } else {
            updateLogisticAddress(bpnA, siteBpn, additionalAddress)
        }

        taskEntryBpnMapping.addMapping(bpnAReference, upsertedBpn)

        // Read the upserted golden record back so the reply carries its full state, including golden record relations.
        return fetchAddressResult(upsertedBpn, hasChanged = true)
    }

    private fun fetchAddressResult(bpnA: String, hasChanged: Boolean?): PostalAddressWithScriptVariants {
        val result = addressService.findAddressByBpn(bpnA)?.let { addressResponseMapper.toAddress(it) }
            ?: throw BpdmValidationException(CleaningError.BPNA_IS_NULL.message)
        return taskResolutionMapper.toTaskResult(result.address, result.scriptVariants, hasChanged)
    }

    private fun createLogisticAddress(
        additionalAddress: PostalAddressWithScriptVariants,
        legalEntityBpn: String,
        siteBpn: String?
    ): String {
        val request = AddressCreateTypedParentsRequest(
            legalEntityBpn = legalEntityBpn,
            siteBpn = siteBpn,
            content = taskAddressRequestMapper.toContentRequest(additionalAddress)
        )

        val result = parseAndExecute(listOf(request), typedParentAddressCreateParser::parse, addressCreateService::create).single()
        return when (result) {
            is ParseResult.Success -> result.parsed.bpn
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { "Errors on creating Address: ${renderError(it)}" })
        }
    }

    private fun updateLogisticAddress(
        bpnA: String,
        siteBpn: String?,
        additionalAddress: PostalAddressWithScriptVariants
    ): String {
        val request = AddressUpdateRequest(
            addressBpn = bpnA,
            siteBpn = siteBpn,
            content = taskAddressRequestMapper.toContentRequest(additionalAddress)
        )

        val result = parseAndExecute(listOf(request), addressUpdateParser::parse, addressPayloadUpdateService::update).single()
        return when (result) {
            is ParseResult.Success -> result.parsed.value.bpn
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { "Errors on updating Address: ${renderError(it)}" })
        }
    }

    // Address parse errors are rendered to messages here (caller-local) so the task path keeps its existing error wording;
    // the field errors reuse the CleaningError texts the old throwing translation produced.
    private fun renderError(error: AddressCreateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
            // Unreachable on the task path: parents arrive already typed, so the untyped-stage InvalidParentBpn never occurs here.
            is InvalidParentBpn -> "Parent ${error.bpn} is not a valid BPNL/BPNS"
            is AddressContentParseError -> renderError(error)
        }

    private fun renderError(error: AddressSiteAssignmentParseError): String =
        when (error) {
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
        }

    private fun renderError(error: AddressUpdateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> renderError(error)
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is AddressContentParseError -> renderError(error)
            is UnresolvableSite -> "Site parent ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
        }

    private fun renderError(error: AddressContentParseError): String =
        when (error) {
            is AddressFieldParseError -> renderFieldError(error)
            is AddressMetadataParseError -> renderMetadataError(error)
            is AddressConstraintParseError -> renderConstraintError(error)
            is AddressScriptVariantParseError -> renderScriptVariantError(error)
        }

    private fun renderFieldError(error: AddressFieldParseError): String =
        when (error) {
            AddressFieldParseError.PhysicalCountryMissing -> CleaningError.PHYSICAL_ADDRESS_COUNTRY_MISSING.message
            AddressFieldParseError.PhysicalCityMissing -> CleaningError.PHYSICAL_ADDRESS_CITY_MISSING.message
            AddressFieldParseError.AlternativeCountryMissing -> CleaningError.ALTERNATIVE_ADDRESS_COUNTRY_MISSING.message
            AddressFieldParseError.AlternativeCityMissing -> CleaningError.ALTERNATIVE_ADDRESS_CITY_MISSING.message
            AddressFieldParseError.AlternativeDeliveryServiceTypeMissing -> CleaningError.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING.message
            AddressFieldParseError.AlternativeDeliveryServiceNumberMissing -> CleaningError.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING.message
            AddressFieldParseError.ConfidenceCriteriaMissing -> CleaningError.ADDRESS_CONFIDENCE_CRITERIA_MISSING.message
            is AddressFieldParseError.CountryCodeNotRecognized -> "Country Code not recognized"
            is AddressFieldParseError.IdentifierValueMissing -> "Identifier value is null"
            is AddressFieldParseError.IdentifierTypeMissing -> "Identifier type is null"
            is AddressFieldParseError.StateTypeMissing -> "Business Partner state type is null"
        }

    private fun renderMetadataError(error: AddressMetadataParseError): String =
        when (error) {
            is AddressMetadataParseError.IdentifierTypeNotFound -> "Address identifier type '${error.type}' is not known"
            is AddressMetadataParseError.PhysicalRegionNotFound -> "Region '${error.regionCode}' in physical address is not known"
            is AddressMetadataParseError.AlternativeRegionNotFound -> "Region '${error.regionCode}' in alternative address is not known"
            is AddressMetadataParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
        }

    private fun renderConstraintError(error: AddressConstraintParseError): String =
        when (error) {
            is AddressConstraintParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is AddressConstraintParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
        }

    private fun renderScriptVariantError(error: AddressScriptVariantParseError): String =
        when (error) {
            is AddressScriptVariantParseError.PhysicalCityMissing -> "Script variant ${error.index} has no city in its physical address"
            is AddressScriptVariantParseError.AlternativeCityMissing -> "Script variant ${error.index} has no city in its alternative address"
            is AddressScriptVariantParseError.DuplicateScriptCode -> "Duplicate address script variant for script code '${error.scriptCode}'"
        }

    private fun renderError(error: LegalEntityCreateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> renderLegalAddressCoverageError(error)
            is LegalEntityContentParseError -> renderError(error)
            is AddressContentParseError -> renderError(error)
        }

    private fun renderError(error: LegalEntityUpdateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is MultipleUltimateOwnersInHierarchy ->
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}"
            is AlternativeHeadquarterCannotOwnUltimately ->
                "Legal entity ${error.bpnl} cannot carry the ultimate-owner flag because it is an alternative headquarter"
            is ScriptVariantCoverageParseError -> renderLegalAddressCoverageError(error)
            is LegalEntityContentParseError -> renderError(error)
            is AddressContentParseError -> renderError(error)
        }

    private fun renderError(error: LegalEntityContentParseError): String =
        when (error) {
            LegalEntityContentParseError.NameMissing -> CleaningError.LEGAL_NAME_IS_NULL.message
            LegalEntityContentParseError.ConfidenceCriteriaMissing -> CleaningError.LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING.message
            is LegalEntityContentParseError.LegalFormNotFound -> "Legal form '${error.legalForm}' is not known"
            is LegalEntityContentParseError.IdentifierValueMissing -> "Identifier value is null"
            is LegalEntityContentParseError.IdentifierTypeMissing -> "Identifier type is null"
            is LegalEntityContentParseError.IdentifierTypeNotFound -> "Legal entity identifier type '${error.type}' is not known"
            is LegalEntityContentParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is LegalEntityContentParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
            is LegalEntityContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing -> "Script variant ${error.index} has no legal name"
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode ->
                "Duplicate legal entity script variant for script code '${error.scriptCode}'"
        }

    private fun renderError(error: SiteCreateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is LegalAddressAlreadyMainAddress -> "Legal address already is the main address of site ${error.bpnSite}"
            is ScriptVariantCoverageParseError -> renderMainAddressCoverageError(error)
            is SiteContentParseError -> renderError(error)
            is AddressContentParseError -> renderError(error)
        }

    private fun renderError(error: SiteUpdateParseError): String =
        when (error) {
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is ScriptVariantCoverageParseError -> renderMainAddressCoverageError(error)
            is SiteContentParseError -> renderError(error)
            is AddressContentParseError -> renderError(error)
        }

    private fun renderError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the address"
            is ScriptVariantCoverageStillNeeded ->
                "Script code '${error.scriptCode}' must stay covered: business partner ${error.requiredByBpn} is named in that script"
        }

    private fun renderLegalAddressCoverageError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the legal address"
            is ScriptVariantCoverageStillNeeded -> renderError(error)
        }

    private fun renderMainAddressCoverageError(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the site main address"
            is ScriptVariantCoverageStillNeeded -> renderError(error)
        }

    private fun renderError(error: SiteContentParseError): String =
        when (error) {
            SiteContentParseError.NameMissing -> CleaningError.SITE_NAME_MISSING.message
            SiteContentParseError.ConfidenceCriteriaMissing -> CleaningError.SITE_CONFIDENCE_CRITERIA_MISSING.message
            is SiteContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is SiteContentParseError.ScriptVariantNameMissing -> "Script variant ${error.index} has no site name"
            is SiteContentParseError.ScriptVariantDuplicateScriptCode -> "Duplicate site script variant for script code '${error.scriptCode}'"
        }

    private fun updateConfidences(
        goldenRecordType: GoldenRecordType,
        sharingMemberRecordId: String,
        legalEntityResult: LegalEntity,
        siteResult: Site?,
        additionalAddressResult: PostalAddressWithScriptVariants?
    ): Triple<LegalEntity, Site?, PostalAddressWithScriptVariants?>{

        val sharingMemberRecordBpnA = recordAddressBpn(goldenRecordType, legalEntityResult, siteResult, additionalAddressResult)

        val updateResults = sharingMemberConfidenceService.updateAddress(sharingMemberRecordId, sharingMemberRecordBpnA)

        val updatedLegalEntityResult =legalEntityResult.withUpdatedNumberOfSharingMembers(updateResults.updatedLegalEntities, updateResults.updatedAddresses)
        val updatedSiteResult = siteResult?.copy(siteMainAddress = siteResult.siteMainAddress?.withUpdatedNumberOfSharingMembers(updateResults.updatedAddresses))
        val updatedAddAddressResult = additionalAddressResult?.copyAsPostalAddress { it.withUpdatedNumberOfSharingMembers(updateResults.updatedAddresses) }

        return Triple(updatedLegalEntityResult, updatedSiteResult, updatedAddAddressResult)
    }


    private fun buildTaskReply(
        taskId: String,
        originalBusinessPartner: BusinessPartner,
        legalEntityResult: LegalEntity,
        siteResult: Site?,
        addressResult: PostalAddressWithScriptVariants?,
        additionalSiteResults: List<AdditionalSite>
    ): TaskStepResultEntryDto{
        //We do this for one special case:
        //Legal Entity has not changed but site has changed and the main address is legal address
        //In this case we want to return the most up-to-date address which is stored in the siteResult
        val isLegalAndSiteMainAddress = siteResult?.siteMainAddress?.bpnReference == legalEntityResult.legalAddress.bpnReference

        val businessPartnerResult = with(originalBusinessPartner){
            copy(
                legalEntity = if(isLegalAndSiteMainAddress) legalEntityResult.copy(legalAddress = siteResult.siteMainAddress!!) else legalEntityResult,
                site = if(isLegalAndSiteMainAddress) siteResult.copy(siteMainAddress = null) else siteResult,
                additionalAddress = addressResult,
                additionalSites = additionalSiteResults
            )
        }

        return TaskStepResultEntryDto(
            taskId = taskId,
            businessPartner = businessPartnerResult,
            errors = emptyList()
        )

    }

    private fun assertParentsConsistent(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping) {
        val addressBpn = businessPartner.additionalAddress?.bpnReference?.let { taskEntryBpnMapping.getBpn(it) }
        val siteBpn = businessPartner.site?.bpnReference?.let { taskEntryBpnMapping.getBpn(it) }
        val legalEntityBpn = taskEntryBpnMapping.getBpn(businessPartner.legalEntity.bpnReference)

        if (siteBpn != null) {
            val foundSite = siteRepository.findByBpn(siteBpn)
            if (foundSite != null) {
                if (foundSite.legalEntity.bpn != legalEntityBpn) {
                    throw BpdmValidationException(CleaningError.SITE_WRONG_LEGAL_ENTITY_REFERENCE.message)
                }
            }
        }

        if (addressBpn != null) {
            val foundAddress = logisticAddressRepository.findByBpn(addressBpn)
            if (foundAddress != null) {
                if (foundAddress.legalEntity!!.bpn != legalEntityBpn) {
                    throw BpdmValidationException(CleaningError.ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE.message)
                }
            }
        }
    }

    private fun PostalAddress.withUpdatedNumberOfSharingMembers(fromCandidates: Collection<LogisticAddressDb>): PostalAddress{
        return copy(
            confidenceCriteria = confidenceCriteria.copy(numberOfSharingMembers = fromCandidates.find { it.bpn == this.bpnReference.referenceValue }?.confidenceCriteria?.numberOfSharingMembers ?: confidenceCriteria.numberOfSharingMembers )
        )
    }

    private fun LegalEntity.withUpdatedNumberOfSharingMembers(legalEntityCandidates: Collection<LegalEntityDb>, legalAddressCandidates: Collection<LogisticAddressDb>): LegalEntity{
        return copy(
            confidenceCriteria = confidenceCriteria.copy(numberOfSharingMembers = legalEntityCandidates.find { it.bpn == this.bpnReference.referenceValue }?.confidenceCriteria?.numberOfSharingMembers ?: confidenceCriteria.numberOfSharingMembers),
            legalAddress = legalAddress.withUpdatedNumberOfSharingMembers(legalAddressCandidates)
        )
    }

    /**
     * The legal address script variants of the script codes [site] does not state itself. A site whose main address is the
     * legal address writes that one address, so its payload has to keep covering what the legal entity is named in -
     * otherwise the last write of the task would decide which of the two partners stays readable.
     */
    private fun BusinessPartner.legalAddressCoverageNotStatedBy(site: Site): List<PostalAddressScriptVariantWithScriptCode> {
        if (!site.siteMainIsLegalAddress) return emptyList()

        val statedScriptCodes = site.scriptVariants.map { it.scriptCode }.toSet()
        return legalEntity.scriptVariants
            .filterNot { it.scriptCode in statedScriptCodes }
            .map { PostalAddressScriptVariantWithScriptCode(it.scriptCode, it.legalAddress) }
    }

    private fun assertScriptVariantCoverage(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping) {
        val violations = coverageValidator.validate(businessPartner, taskEntryBpnMapping)
        if (violations.isNotEmpty()) throw BpdmMultiValidationException(violations.map { renderError(it) })
    }

    private fun Site.withRelevantScriptVariants(businessPartner: BusinessPartner): Site {
        if (!siteMainIsLegalAddress) return this

        // The main address is the legal address, so its script variants are stored on the legal entity, not on the site.
        val legalAddressVariantsByCode = businessPartner.legalEntity.scriptVariants.associate { it.scriptCode to it.legalAddress }

        return copy(
            scriptVariants = scriptVariants.map { variant ->
                variant.copy(mainAddress = legalAddressVariantsByCode[variant.scriptCode] ?: variant.mainAddress)
            }
        )
    }
}