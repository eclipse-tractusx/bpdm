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
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.parseAndExecute
import org.eclipse.tractusx.bpdm.common.model.parseAndExecuteAllOrNone
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmMultiValidationException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskAddressRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskLegalEntityRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.GoldenRecordTaskSiteRequestMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.GoldenRecordTaskResultMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressResponseMapper
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.SiteResponseMapper
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSiteMembershipRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.address.AddressUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.legalentity.LegalEntityPayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressSiteMembershipParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.TypedParentAddressCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.*
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordTaskCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.task.GoldenRecordTaskSiteMainAddressValidator
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service


@Service
class TaskStepBuildService(
    private val businessPartnerFetchService: BusinessPartnerFetchService,
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    private val taskResultMapper: GoldenRecordTaskResultMapper,
    private val addressResponseMapper: AddressResponseMapper,
    private val siteResponseMapper: SiteResponseMapper,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val siteRepository: SiteRepository,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val addressBpnParser: AddressBpnParser,
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
    private val addressSiteMembershipParser: AddressSiteMembershipParser,
    private val addressUpdateService: AddressUpdateService,
    private val taskLegalEntityRequestMapper: GoldenRecordTaskLegalEntityRequestMapper,
    private val taskSiteRequestMapper: GoldenRecordTaskSiteRequestMapper,
    private val coverageValidator: GoldenRecordTaskCoverageValidator,
    private val siteMainAddressValidator: GoldenRecordTaskSiteMainAddressValidator,
    private val parseErrorMapper: GoldenRecordTaskParseErrorMapper
) {

    @Transactional
    fun upsertBusinessPartner(taskEntry: TaskStepReservationEntryDto): TaskStepResultEntryDto {
        val taskEntryBpnMapping = TaskEntryBpnMapping(listOf(taskEntry), bpnRequestIdentifierRepository)
        val businessPartnerDto = taskEntry.businessPartner

        assertParentsConsistent(businessPartnerDto, taskEntryBpnMapping)
        assertScriptVariantCoverage(businessPartnerDto, taskEntryBpnMapping)
        assertSiteMainAddressStated(businessPartnerDto, taskEntryBpnMapping)

        val legalEntityResult = processLegalEntity(businessPartnerDto, taskEntryBpnMapping)
        val siteResult = processSite(businessPartnerDto, legalEntityResult.bpnReference.referenceValue!!, taskEntryBpnMapping)
        val addressResult = processAdditionalAddress(businessPartnerDto, legalEntityResult.bpnReference.referenceValue!!, siteResult?.bpnReference?.referenceValue, taskEntryBpnMapping)

        // The address the additional sites attach to only exists once its own golden record has been written, so this
        // runs after all three components.
        val recordAddressBpn = recordAddressBpn(businessPartnerDto.type!!, legalEntityResult, siteResult, addressResult)
        val recordSiteBpn = siteResult?.bpnReference?.referenceValue
        processAdditionalSites(businessPartnerDto, recordSiteBpn, recordAddressBpn, taskEntryBpnMapping)
        val additionalSiteResults = recordSiteBpn?.let { readAdditionalSites(recordAddressBpn, it) }.orEmpty()

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
        recordSiteBpn: String?,
        recordAddressBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ) {
        // Additional sites are the sites of the address next to the site this data is about, so business partner data
        // without a site of its own states nothing about the membership and leaves it as it stands - the same rule the
        // data has to satisfy on its way in.
        if (recordSiteBpn == null) return

        // The same site stated twice is one statement written twice, not two memberships. An entry is identified by the
        // reference it carries and, carrying none, by the name its site is to be created under - as far as identity goes
        // here: resolving a name to an existing site is the refinement service's job, not this one's.
        val statedOnce = businessPartner.additionalSites.distinctBy { it.bpnReference.referenceValue ?: it.siteName }
        val (known, unknown) = statedOnce.partition { taskEntryBpnMapping.getBpn(it.bpnReference) != null }

        val createdSiteBpns = createAdditionalSites(unknown, businessPartner, recordAddressBpn, taskEntryBpnMapping)
        val knownSiteBpns = known.map { taskEntryBpnMapping.getBpn(it.bpnReference)!! }

        val completeMembership = (listOf(recordSiteBpn) + knownSiteBpns + createdSiteBpns).distinct()
        setAddressSites(recordAddressBpn, completeMembership)
    }

    private fun createAdditionalSites(
        additionalSites: List<AdditionalSite>,
        businessPartner: BusinessPartner,
        recordAddressBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): List<String> {
        if (additionalSites.isEmpty()) return emptyList()

        val confidenceCriteria = additionalSiteConfidence(businessPartner)
        val requests = additionalSites.map { taskSiteRequestMapper.toCreateOnAddressRequest(recordAddressBpn, it, confidenceCriteria) }

        val createdSites = parseAndExecuteAllOrNone(
            requests,
            siteCreateOnAddressParser::parse,
            { errors -> BpdmMultiValidationException(errors.map { parseErrorMapper.toSiteCreateDescription(it) }) },
            siteCreateWithReferencedAddressAsMainService::create
        )

        additionalSites.zip(createdSites).forEach { (additionalSite, createdSite) ->
            taskEntryBpnMapping.addMapping(additionalSite.bpnReference, createdSite.bpn)
        }

        return createdSites.map { it.bpn }
    }

    private fun setAddressSites(recordAddressBpn: String, siteBpns: List<String>) {
        parseAndExecuteAllOrNone(
            listOf(AddressSiteMembershipRequest(addressBpn = recordAddressBpn, siteBpns = siteBpns)),
            addressSiteMembershipParser::parse,
            { errors -> BpdmMultiValidationException(errors.map { parseErrorMapper.toAddressSiteMembershipDescription(it) }) },
            addressUpdateService::setSites
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
        logisticAddressRepository.findByBpn(recordAddressBpn)
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

        val existingLegalEntityInformation by lazy { fetchExistingLegalEntityResult(bpnL!!) }

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
        return readUpsertedLegalEntityResult(upsertedLegalEntity.bpn)
    }

    private fun createLegalEntity(legalEntity: LegalEntity): LegalEntityDb {
        val request = taskLegalEntityRequestMapper.toCreateRequest(legalEntity)
        return when (val result = parseAndExecute(listOf(request), legalEntityCreateParser::parse, legalEntityCreateService::create).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { parseErrorMapper.toLegalEntityCreateDescription(it) })
        }
    }

    private fun updateLegalEntity(bpnL: String, legalEntity: LegalEntity): LegalEntityDb {
        val request = taskLegalEntityRequestMapper.toUpdateRequest(bpnL, legalEntity)
        return when (val result = parseAndExecute(listOf(request), legalEntityUpdateParser::parseWithoutCoverageCheck, legalEntityPayloadUpdateService::update).single()) {
            is ParseResult.Success -> result.parsed.value
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { parseErrorMapper.toLegalEntityUpdateDescription(it) })
        }
    }

    private fun fetchExistingLegalEntityResult(bpnL: String): LegalEntity =
        readLegalEntityResult(bpnL, hasChanged = false)
            ?: throw BpdmValidationException("Legal entity with specified BPNL $bpnL not found")

    private fun readUpsertedLegalEntityResult(bpnL: String): LegalEntity =
        readLegalEntityResult(bpnL, hasChanged = true)
            ?: error("Legal entity $bpnL was written by this task but cannot be read back")

    private fun readLegalEntityResult(bpnL: String, hasChanged: Boolean?): LegalEntity? =
        businessPartnerFetchService.fetchDtosByBpns(listOf(bpnL)).firstOrNull()
            ?.let { taskResultMapper.toTaskResult(it, hasChanged) }

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
            fetchExistingSiteResult(bpnS)
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
        val address = resolveAddressIfPresent(bpnA)
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
        val siteMainAddress = site.siteMainAddress ?: error("Site to create on address ${existingAddress.bpn} states no main address")
        val bpnSReference = site.bpnReference
        val mergedSite = site.withRelevantScriptVariants(businessPartner)

        val request = taskSiteRequestMapper.toCreateWithReferencedAddressAsMainRequest(existingAddress.bpn, mergedSite, siteMainAddress)
        val createdSite = when (val result = parseAndExecute(listOf(request), siteCreateWithReferencedAddressAsMainParser::parse, siteCreateWithReferencedAddressAsMainService::create).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { parseErrorMapper.toSiteCreateDescription(it) })
        }

        taskEntryBpnMapping.addMapping(bpnSReference, createdSite.bpn)
        taskEntryBpnMapping.addMapping(siteMainAddress.bpnReference, createdSite.mainAddress.bpn)
        return readUpsertedSiteResult(createdSite.bpn)
    }

    private fun upsertSite(
        site: Site,
        businessPartner: BusinessPartner,
        legalEntityBpn: String,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): Site {
        val isSiteMainAndLegalAddress = site.siteMainIsLegalAddress
        val siteMainAddress = if(isSiteMainAndLegalAddress) businessPartner.legalEntity.legalAddress
            else (site.siteMainAddress ?: error("Site ${site.bpnReference.referenceValue} states no main address"))

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

        return readUpsertedSiteResult(upsertedSite.bpn)
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
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { parseErrorMapper.toSiteCreateDescription(it) })
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
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { parseErrorMapper.toSiteUpdateDescription(it) })
        }
    }

    // A site the task states but does not write is rejected with the main-address wording the throwing translation
    // produced for it; a proper unresolvable-site error waits for the site BPN to be resolved by a parser.
    private fun fetchExistingSiteResult(bpnS: String): Site =
        readSiteResult(bpnS, hasChanged = false)
            ?: throw BpdmValidationException(GoldenRecordTaskErrorMessage.MAINE_ADDRESS_IS_NULL.message)

    private fun readUpsertedSiteResult(bpnS: String): Site =
        readSiteResult(bpnS, hasChanged = true)
            ?: error("Site $bpnS was written by this task but cannot be read back")

    private fun readSiteResult(bpnS: String, hasChanged: Boolean?): Site? =
        siteRepository.findByBpn(bpnS)?.let { siteResponseMapper.toSiteWithMainAddress(it) }
            ?.let { taskResultMapper.toTaskResult(it.site, it.mainAddress, hasChanged) }

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
            toAddressResult(resolveRequestedAddress(bpnA), hasChanged = false)
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
            updateLogisticAddress(bpnA, additionalAddress)
        }

        taskEntryBpnMapping.addMapping(bpnAReference, upsertedBpn)

        // Read the upserted golden record back so the reply carries its full state, including golden record relations.
        return toAddressResult(readUpsertedAddress(upsertedBpn), hasChanged = true)
    }

    // The resolvers are strict, so an unknown BPN comes back as a failure; here that is not a rejection but the signal
    // that this main-address reference names no address yet, which the caller answers by creating one.
    private fun resolveAddressIfPresent(bpnA: String): LogisticAddressDb? =
        when (val result = addressBpnParser.parse(listOf(bpnA)).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> null
        }

    private fun resolveRequestedAddress(bpnA: String): LogisticAddressDb =
        when (val result = addressBpnParser.parse(listOf(bpnA)).single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { "Address ${it.bpn} not found" })
        }

    private fun readUpsertedAddress(bpnA: String): LogisticAddressDb =
        logisticAddressRepository.findByBpn(bpnA) ?: error("Address $bpnA was written by this task but cannot be read back")

    private fun toAddressResult(address: LogisticAddressDb, hasChanged: Boolean?): PostalAddressWithScriptVariants {
        val result = addressResponseMapper.toAddress(address)
        return taskResultMapper.toTaskResult(result.address, result.scriptVariants, hasChanged)
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
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { "Errors on creating Address: ${parseErrorMapper.toAddressCreateDescription(it)}" })
        }
    }

    private fun updateLogisticAddress(
        bpnA: String,
        additionalAddress: PostalAddressWithScriptVariants
    ): String {
        // Site membership is stated once for the whole record, by processAdditionalSites, so this update leaves it alone.
        val request = AddressUpdateRequest(
            addressBpn = bpnA,
            siteBpns = null,
            content = taskAddressRequestMapper.toContentRequest(additionalAddress)
        )

        val result = parseAndExecute(listOf(request), addressUpdateParser::parse, addressPayloadUpdateService::update).single()
        return when (result) {
            is ParseResult.Success -> result.parsed.value.bpn
            is ParseResult.Failure -> throw BpdmMultiValidationException(result.errors.map { "Errors on updating Address: ${parseErrorMapper.toAddressUpdateDescription(it)}" })
        }
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
                    throw BpdmValidationException(GoldenRecordTaskErrorMessage.SITE_WRONG_LEGAL_ENTITY_REFERENCE.message)
                }
            }
        }

        if (addressBpn != null) {
            val foundAddress = logisticAddressRepository.findByBpn(addressBpn)
            if (foundAddress != null) {
                if (foundAddress.legalEntity!!.bpn != legalEntityBpn) {
                    throw BpdmValidationException(GoldenRecordTaskErrorMessage.ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE.message)
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

    private fun assertSiteMainAddressStated(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping) {
        val violations = siteMainAddressValidator.validate(businessPartner, taskEntryBpnMapping)
        if (violations.isNotEmpty()) throw BpdmMultiValidationException(violations.map { parseErrorMapper.toTaskDescription(it) })
    }

    private fun assertScriptVariantCoverage(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping) {
        val violations = coverageValidator.validate(businessPartner, taskEntryBpnMapping)
        if (violations.isNotEmpty()) throw BpdmMultiValidationException(violations.map { parseErrorMapper.toScriptVariantCoverageDescription(it) })
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
