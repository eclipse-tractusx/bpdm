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

package org.eclipse.tractusx.bpdm.test.system.utils

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.GivenConfidence
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import tools.jackson.databind.json.JsonMapper

class BusinessPartnerShareActions(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val testDataGenerator: ShareOwnCompanyDataTestDataGenerator,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val jsonMapper: JsonMapper
) {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!

    fun upload(recordId: String, isOwnCompanyData: Boolean, contentSeed: String = recordId) {
        val inputData = testDataGenerator.buildInputData(contentSeed).copy(isOwnCompanyData = isOwnCompanyData)
        val runId = context.runId(recordId)
        val request = listOf(inputData.copy(externalId = runId))
        val response = gateClient.businessParters.upsertBusinessPartnersInput(request)
        attachApiCall("PUT", "/v7/input/business-partners", request, response.body)
        context.records[recordId] = (context.records[recordId] ?: RecordState()).copy(
            contentSeed = contentSeed,
            currentInput = inputData
        )
    }

    fun refineAsLegalEntity(recordId: String, verified: Boolean) {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildLegalEntity(state.contentSeed!!, givenConfidence(state, verified))
        resolveTask(recordId, entityResult.taskData)
        context.records[recordId] = state.copy(legalEntity = entityResult.legalEntity)
    }

    /**
     * Refines a record into a legal entity whose master data is built from [masterDataSeed], independent
     * of the seed the record was shared with, and waits for the golden record process to complete so the
     * resulting output and Pool golden record are available. The golden record identity is keyed by
     * [legalEntityLabel] (not by the seed), so refining a different record to the same label lands on the same
     * Pool golden record and updates its master data. Returns the golden record so the caller can store it as
     * the current expectation for that legal entity.
     */
    fun refineAsLegalEntity(
        recordId: String,
        masterDataSeed: String,
        legalEntityLabel: String,
        verified: Boolean = false
    ): LegalEntityWithLegalAddressVerboseDto {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildLegalEntity(masterDataSeed, givenConfidence(state, verified))
        resolveTask(recordId, entityResult.taskData.withGoldenRecordRequestIdentifiers(legalEntityLabel))
        context.records[recordId] = state.copy(legalEntity = entityResult.legalEntity)
        sharingStateWatcher.waitForCompletedState(recordId)
        return entityResult.legalEntity
    }

    /**
     * Refines a record into a legal entity exactly like the label overload above, but pins the legal entity's
     * single script variant to [scriptCode] (on both the resulting golden record and the refinement task, so
     * they stay consistent). This lets a scenario give the legal entity a known, distinct script code - needed
     * when a later additional address of the same legal entity carries a different code and the merged output
     * must show both. Returns the golden record so the caller can store it as the current expectation.
     */
    fun refineAsLegalEntity(
        recordId: String,
        masterDataSeed: String,
        legalEntityLabel: String,
        verified: Boolean,
        scriptCode: String
    ): LegalEntityWithLegalAddressVerboseDto {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildLegalEntity(masterDataSeed, givenConfidence(state, verified)).let {
            it.copy(
                legalEntity = it.legalEntity.withScriptCode(scriptCode),
                taskData = it.taskData.withLegalEntityScriptCode(scriptCode)
            )
        }
        resolveTask(recordId, entityResult.taskData.withGoldenRecordRequestIdentifiers(legalEntityLabel))
        context.records[recordId] = state.copy(legalEntity = entityResult.legalEntity)
        sharingStateWatcher.waitForCompletedState(recordId)
        return entityResult.legalEntity
    }

    fun refineAsSiteBasedLegalEntity(recordId: String, verified: Boolean) {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildSiteBasedLegalEntity(state.contentSeed!!, givenConfidence(state, verified))
        resolveTask(recordId, entityResult.taskData)
        context.records[recordId] = state.copy(
            legalEntity = entityResult.siteBasedLegalEntity.legalEntity,
            poolSite = SiteWithMainAddressVerboseDto(
                entityResult.siteBasedLegalEntity.site,
                entityResult.siteBasedLegalEntity.legalEntity.legalAddress
            )
        )
    }

    /**
     * Refines a record into a site-based legal entity (a legal entity whose legal address is also its site's main
     * address) whose master data is built from [masterDataSeed], independent of the seed the record was shared
     * with, and waits for the golden record process to complete so the resulting output and Pool golden record are
     * available. The golden record identities are keyed by [legalEntityLabel] and [siteLabel] (not by the seed), so
     * refining a different record to the same labels lands on the same Pool golden records and updates their master
     * data. Returns the site together with its parent legal entity so the caller can store them as the current
     * expectation.
     */
    fun refineAsSiteBasedLegalEntity(
        recordId: String,
        masterDataSeed: String,
        siteLabel: String,
        legalEntityLabel: String,
        verified: Boolean = false
    ): SiteBasedLegalEntity {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildSiteBasedLegalEntity(masterDataSeed, givenConfidence(state, verified))
        resolveTask(recordId, entityResult.taskData.withGoldenRecordRequestIdentifiers(legalEntityLabel, siteLabel = siteLabel))
        context.records[recordId] = state.copy(
            legalEntity = entityResult.siteBasedLegalEntity.legalEntity,
            poolSite = SiteWithMainAddressVerboseDto(
                entityResult.siteBasedLegalEntity.site,
                entityResult.siteBasedLegalEntity.legalEntity.legalAddress
            )
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return entityResult.siteBasedLegalEntity
    }

    fun refineAsSite(recordId: String) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent")
        val siteResult = testDataGenerator.buildSite(contentSeed, parentResult.legalEntity)
        resolveTask(recordId, siteResult.taskData)
        context.records[recordId] = state.copy(
            legalEntity = siteResult.siteWithParent.legalEntity,
            poolSite = siteResult.siteWithParent.site
        )
    }

    /**
     * Refines a record into a site (and its parent legal entity) whose master data is built from
     * [masterDataSeed], independent of the seed the record was shared with, and waits for the golden record
     * process to complete so the resulting output and Pool golden record are available. The golden record
     * identities are keyed by [siteLabel] and its parent [legalEntityLabel] (not by the seed), so refining a
     * different record to the same labels lands on the same Pool golden records and updates their master data.
     * Returns the site together with its parent legal entity so the caller can store them as the current
     * expectation.
     */
    fun refineAsSite(
        recordId: String,
        masterDataSeed: String,
        siteLabel: String,
        legalEntityLabel: String
    ): SiteWithParent {
        val state = context.records[recordId]!!
        val parentResult = testDataGenerator.buildLegalEntity("${masterDataSeed}Parent")
        val siteResult = testDataGenerator.buildSite(masterDataSeed, parentResult.legalEntity)
        resolveTask(recordId, siteResult.taskData.withGoldenRecordRequestIdentifiers(legalEntityLabel, siteLabel = siteLabel))
        context.records[recordId] = state.copy(
            legalEntity = siteResult.siteWithParent.legalEntity,
            poolSite = siteResult.siteWithParent.site
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return siteResult.siteWithParent
    }

    /**
     * Refines a record into a site exactly like the label overload above, but pins the site's MAIN ADDRESS
     * to the shared [mainAddressLabel] instead of the site label. Two records refined to distinct
     * [siteLabel]s under the same [legalEntityLabel] but the same [mainAddressLabel] therefore ask the Pool
     * to make both sites share one main address. Returns the site together with its parent legal entity so
     * the caller can store them as the current expectation.
     */
    fun refineAsSite(
        recordId: String,
        masterDataSeed: String,
        siteLabel: String,
        legalEntityLabel: String,
        mainAddressLabel: String
    ): SiteWithParent {
        val state = context.records[recordId]!!
        val parentResult = testDataGenerator.buildLegalEntity("${masterDataSeed}Parent")
        val siteResult = testDataGenerator.buildSite(masterDataSeed, parentResult.legalEntity)
        resolveTask(
            recordId,
            siteResult.taskData.withGoldenRecordRequestIdentifiers(
                legalEntityLabel,
                siteLabel = siteLabel,
                siteMainAddressLabel = mainAddressLabel
            )
        )
        context.records[recordId] = state.copy(
            legalEntity = siteResult.siteWithParent.legalEntity,
            poolSite = siteResult.siteWithParent.site
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return siteResult.siteWithParent
    }

    fun refineAsAdditionalAddressOfLegalEntity(recordId: String, verified: Boolean) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        // The parent legal entity is determined by the golden record process, not shared by the owner, so it
        // is built without the owner signal. Only the resulting additional address carries it.
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent", TestDataV7.NotCheckedNotOwned)
        val addressResult = testDataGenerator.buildAdditionalLegalEntityAddress(contentSeed, parentResult.legalEntity, givenConfidence(state, verified))
        resolveTask(recordId, addressResult.taskData)
        context.records[recordId] = state.copy(
            legalEntity = addressResult.additionalLegalEntityAddressWithParent.legalEntity,
            poolAddress = addressResult.additionalLegalEntityAddressWithParent.address
        )
    }

    /**
     * Refines a record into an additional address of a legal entity whose master data is built from
     * [masterDataSeed], independent of the seed the record was shared with, and waits for the golden record
     * process to complete so the resulting output and Pool golden record are available. The golden record
     * identities are keyed by [additionalAddressLabel] and its parent [legalEntityLabel] (not by the seed), so
     * refining a different record to the same labels lands on the same Pool golden records and updates their
     * master data. Returns the address together with its parent legal entity so the caller can store them as
     * the current expectation.
     */
    fun refineAsAdditionalAddressOfLegalEntity(
        recordId: String,
        masterDataSeed: String,
        additionalAddressLabel: String,
        legalEntityLabel: String,
        verified: Boolean = false
    ): AdditionalLegalEntityAddressWithParent {
        val state = context.records[recordId]!!
        // The parent legal entity is determined by the golden record process, not shared by the owner, so it
        // is built without the owner signal. Only the resulting additional address carries it.
        val parentResult = testDataGenerator.buildLegalEntity("${masterDataSeed}Parent", TestDataV7.NotCheckedNotOwned)
        val addressResult = testDataGenerator.buildAdditionalLegalEntityAddress(masterDataSeed, parentResult.legalEntity, givenConfidence(state, verified))
        resolveTask(recordId, addressResult.taskData.withGoldenRecordRequestIdentifiers(legalEntityLabel, additionalAddressLabel = additionalAddressLabel))
        context.records[recordId] = state.copy(
            legalEntity = addressResult.additionalLegalEntityAddressWithParent.legalEntity,
            poolAddress = addressResult.additionalLegalEntityAddressWithParent.address
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return addressResult.additionalLegalEntityAddressWithParent
    }

    /**
     * Refines a record into an additional address of an *existing* legal entity golden record - the one stored
     * under [parentLegalEntityLabel] by an earlier refinement - instead of building a fresh parent. The parent
     * keeps its own (already shared) script variant, and the new additional address gets its single script
     * variant pinned to [scriptCode]. Resolving with the parent's request identifier makes the Pool match the
     * existing parent and merely add the new address, so the parent's script variant is preserved. This is what
     * the merge scenario needs: a legal entity carrying script code A and one of its additional addresses
     * carrying a different script code B, whose merged output then shows both. Returns the address together with
     * its parent so the caller can store it as the current expectation.
     */
    fun refineAsAdditionalAddressOfExistingLegalEntity(
        recordId: String,
        masterDataSeed: String,
        additionalAddressLabel: String,
        parentLegalEntityLabel: String,
        scriptCode: String
    ): AdditionalLegalEntityAddressWithParent {
        val state = context.records[recordId]!!
        val parentLegalEntity = context.legalEntities[parentLegalEntityLabel]
            ?: error("legal entity '$parentLegalEntityLabel' must be defined by an earlier refinement step")
        val addressResult = testDataGenerator
            .buildAdditionalLegalEntityAddress(masterDataSeed, parentLegalEntity, givenConfidence(state, verified = false))
            .let {
                it.copy(
                    additionalLegalEntityAddressWithParent = it.additionalLegalEntityAddressWithParent.copy(
                        address = it.additionalLegalEntityAddressWithParent.address.withScriptCode(scriptCode)
                    ),
                    taskData = it.taskData.withAdditionalAddressScriptCode(scriptCode)
                )
            }
        resolveTask(
            recordId,
            addressResult.taskData.withGoldenRecordRequestIdentifiers(parentLegalEntityLabel, additionalAddressLabel = additionalAddressLabel)
        )
        context.records[recordId] = state.copy(
            legalEntity = addressResult.additionalLegalEntityAddressWithParent.legalEntity,
            poolAddress = addressResult.additionalLegalEntityAddressWithParent.address
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return addressResult.additionalLegalEntityAddressWithParent
    }

    fun refineAsAdditionalAddressOfSite(recordId: String, verified: Boolean) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        // The parent legal entity is determined by the golden record process, not shared by the owner, so it
        // is built without the owner signal. Only the resulting additional address carries it (the site keeps
        // its own always-OwnerShared confidence).
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent", TestDataV7.NotCheckedNotOwned)
        val siteResult = testDataGenerator.buildSite("${contentSeed}Site", parentResult.legalEntity)
        val addressResult = testDataGenerator.buildAdditionalSiteAddress(contentSeed, siteResult.siteWithParent, givenConfidence(state, verified))
        resolveTask(recordId, addressResult.taskData)
        context.records[recordId] = state.copy(
            legalEntity = addressResult.additionalSiteAddressWithParent.siteWithParent.legalEntity,
            poolSite = addressResult.additionalSiteAddressWithParent.siteWithParent.site,
            poolAddress = addressResult.additionalSiteAddressWithParent.address
        )
    }

    /**
     * Refines a record into an additional address of a site (and its parent site and legal entity) whose
     * master data is built from [masterDataSeed], independent of the seed the record was shared with, and waits
     * for the golden record process to complete so the resulting output and Pool golden record are available.
     * The golden record identities are keyed by [additionalAddressLabel], its parent [siteLabel] and the
     * [legalEntityLabel] (not by the seed), so refining a different record to the same labels lands on the same
     * Pool golden records and updates their master data. Returns the address together with its parent site and
     * legal entity so the caller can store them as the current expectation.
     */
    fun refineAsAdditionalAddressOfSite(
        recordId: String,
        masterDataSeed: String,
        additionalAddressLabel: String,
        siteLabel: String,
        legalEntityLabel: String
    ): AdditionalSiteAddressWithParent {
        val state = context.records[recordId]!!
        // The parent legal entity is determined by the golden record process, not shared by the owner, so it
        // is built without the owner signal. Only the resulting additional address carries it (the site keeps
        // its own always-OwnerShared confidence).
        val parentResult = testDataGenerator.buildLegalEntity("${masterDataSeed}Parent", TestDataV7.NotCheckedNotOwned)
        val siteResult = testDataGenerator.buildSite("${masterDataSeed}Site", parentResult.legalEntity)
        val addressResult = testDataGenerator.buildAdditionalSiteAddress(masterDataSeed, siteResult.siteWithParent)
        resolveTask(
            recordId,
            addressResult.taskData.withGoldenRecordRequestIdentifiers(
                legalEntityLabel,
                siteLabel = siteLabel,
                additionalAddressLabel = additionalAddressLabel
            )
        )
        context.records[recordId] = state.copy(
            legalEntity = addressResult.additionalSiteAddressWithParent.siteWithParent.legalEntity,
            poolSite = addressResult.additionalSiteAddressWithParent.siteWithParent.site,
            poolAddress = addressResult.additionalSiteAddressWithParent.address
        )
        sharingStateWatcher.waitForCompletedState(recordId)
        return addressResult.additionalSiteAddressWithParent
    }

    fun completeShare(recordId: String, refine: (String) -> Unit) {
        refine(recordId)
        sharingStateWatcher.waitForCompletedState(recordId)
    }

    fun completeShareAsAdditionalAddressOf(
        recordId: String,
        parentLegalEntity: LegalEntityWithLegalAddressVerboseDto
    ) {
        val state = context.records[recordId]!!
        val addressResult = testDataGenerator.buildAdditionalLegalEntityAddress(state.contentSeed!!, parentLegalEntity, givenConfidence(state, verified = false))
        resolveTask(recordId, addressResult.taskData)
        context.records[recordId] = state.copy(
            legalEntity = parentLegalEntity,
            poolAddress = addressResult.additionalLegalEntityAddressWithParent.address
        )
        sharingStateWatcher.waitForCompletedState(recordId)
    }

    private fun resolveTask(recordId: String, taskData: BusinessPartner) {
        val runId = context.runId(recordId)
        sharingStateWatcher.waitForTaskId(recordId)
        val sharingStatePage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(runId))
        attachApiCall("GET", "/v7/business-partners/sharing-state", mapOf("externalIds" to listOf(runId)), sharingStatePage)
        val taskId = sharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedTask(taskId)
        orchestratorClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, taskData)))
        )
    }

    private fun givenConfidence(state: RecordState, verified: Boolean): GivenConfidence =
        GivenConfidence(
            sharedByOwner = state.currentInput?.isOwnCompanyData ?: false,
            checkedByExternalDataSource = verified
        )

    // -------------------------------------------------------------------------
    // Script code pinning
    //
    // Each generated golden record carries a single script variant whose code is otherwise derived
    // pseudo-randomly from the seed. These helpers rewrite only that code - on the Pool golden record DTO and
    // on the matching orchestrator refinement task in lock-step - so a scenario can give two related entities
    // distinct, known script codes. The derived variant values are intentionally left untouched: the
    // assertions only require the task and the expected golden record to agree, and rewriting both together
    // keeps them so.
    // -------------------------------------------------------------------------

    private fun LegalEntityWithLegalAddressVerboseDto.withScriptCode(scriptCode: String): LegalEntityWithLegalAddressVerboseDto =
        copy(scriptVariants = scriptVariants.map { it.copy(scriptCode = scriptCode) })

    private fun LogisticAddressVerboseDto.withScriptCode(scriptCode: String): LogisticAddressVerboseDto =
        copy(scriptVariants = scriptVariants.map { it.copy(scriptCode = scriptCode) })

    private fun BusinessPartner.withLegalEntityScriptCode(scriptCode: String): BusinessPartner =
        copy(legalEntity = legalEntity.copy(scriptVariants = legalEntity.scriptVariants.map { it.copy(scriptCode = scriptCode) }))

    private fun BusinessPartner.withAdditionalAddressScriptCode(scriptCode: String): BusinessPartner {
        val address = additionalAddress ?: return this
        return copy(additionalAddress = address.copy(scriptVariants = address.scriptVariants.map { it.copy(scriptCode = scriptCode) }))
    }

    /**
     * Rewrites the golden record BPN references in the refinement task as BPN request identifiers derived from
     * the stable entity labels instead of the per-refinement master data seed. The Pool resolves a known
     * request identifier to the same BPN and updates that golden record (see [BpnReference]), so two records
     * refined to the same labels land on the same Pool golden records. This is what lets one record's
     * refinement change the master data of a golden record that another record already reflects. The inline
     * response BPNs are left untouched on purpose: assertions ignore them and re-check the BPNs separately
     * against the Pool.
     */
    private fun BusinessPartner.withGoldenRecordRequestIdentifiers(
        legalEntityLabel: String,
        siteLabel: String? = null,
        additionalAddressLabel: String? = null,
        siteMainAddressLabel: String? = null
    ): BusinessPartner =
        copy(
            legalEntity = legalEntity.copy(
                bpnReference = requestIdentifier("BPNL", legalEntityLabel),
                legalAddress = legalEntity.legalAddress.copy(bpnReference = requestIdentifier("BPNA", legalEntityLabel))
            ),
            site = siteLabel?.let { label ->
                site?.copy(
                    bpnReference = requestIdentifier("BPNS", label),
                    // The site main address is keyed by [siteMainAddressLabel] when given, so two distinct sites
                    // can point their main address at the SAME address request identifier (and thus share it);
                    // it defaults to the site label, keeping each site's main address its own.
                    siteMainAddress = site?.siteMainAddress?.copy(bpnReference = requestIdentifier("BPNA", siteMainAddressLabel ?: label))
                )
            } ?: site,
            additionalAddress = additionalAddressLabel?.let { label ->
                additionalAddress?.copyAsPostalAddress { it.copy(bpnReference = requestIdentifier("BPNA", label)) }
            } ?: additionalAddress
        )

    private fun requestIdentifier(prefix: String, label: String): BpnReference =
        BpnReference("$prefix-request-${context.runId(label)}", null, BpnReferenceType.BpnRequestIdentifier)

    private fun attachApiCall(method: String, path: String, request: Any? = null, response: Any? = null) {
        val content = buildMap {
            put("uri", "$method $path")
            if (request != null) put("request", request)
            if (response != null) put("response", response)
        }
        context.scenario.attach(
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content),
            "application/json",
            "$method $path"
        )
    }
}
