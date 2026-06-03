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

package org.eclipse.tractusx.bpdm.test.system.stepdefinations

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.system.utils.RecordState
import org.eclipse.tractusx.bpdm.test.system.utils.RelationState
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.ShareOwnCompanyDataTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withExternalId
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class ShareOwnCompanyPartnersStepDefs(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val testRunData: TestRunData,
    private val testDataGenerator: ShareOwnCompanyDataTestDataGenerator,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertRepository: GateAssertRepositoryV7,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    @Before
    fun setUp(scenario: Scenario) {
        ScenarioContext.set(ScenarioContext(scenario.name, scenario.id, testRunData.testTime, scenario))
        logger.info { "Starting scenario: '${scenario.name}'" }
    }

    @After
    fun tearDown() {
        ScenarioContext.clear()
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    // The compound Given drives a full share cycle internally to establish the
    // precondition. The Then steps only care about what happens *after* the When.
    @Given("{string} has already been shared as a legal entity")
    fun `given already shared as legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Given: '$recordId' has already been shared as a legal entity" }
        upload(recordId, contentSeed = recordId)
        completeShare(recordId, ::refineAsLegalEntity)
    }

    // Reads the parent legal entity from the earlier Given so the additional address
    // is bound to the correct legal entity without re-generating it.
    @Given("{string} has already been shared as an additional address of {string}'s legal entity")
    fun `given already shared as additional address of legal entity`(addressRecordId: String, legalEntityRecordId: String) {
        logger.info { "[$scenarioName] Given: '$addressRecordId' has already been shared as an additional address of '$legalEntityRecordId''s legal entity" }
        val parentLegalEntity = context.records[legalEntityRecordId]?.legalEntity
            ?: error("'$legalEntityRecordId' must appear in an earlier Given step and be refined as a legal entity")
        upload(addressRecordId, contentSeed = addressRecordId)
        completeShareAsAdditionalAddressOf(addressRecordId, parentLegalEntity)
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("a sharing member uploads their own company data as record {string}")
    fun `when upload own company data`(recordId: String) {
        logger.info { "[$scenarioName] When: sharing member uploads own company data as record '$recordId'" }
        upload(recordId, contentSeed = recordId)
    }

    // Uses a different content seed so the structural data differs from the initial upload.
    @When("a sharing member re-uploads {string} with different content")
    fun `when reupload with different content`(recordId: String) {
        logger.info { "[$scenarioName] When: sharing member re-uploads '$recordId' with different content" }
        upload(recordId, contentSeed = "${recordId}Updated")
    }

    @When("the cleaning service refines {string} as a legal entity")
    fun `when refine as legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service refines '$recordId' as a legal entity" }
        refineAsLegalEntity(recordId)
    }

    @When("the cleaning service refines {string} as a site-based legal entity")
    fun `when refine as site-based legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service refines '$recordId' as a site-based legal entity" }
        refineAsSiteBasedLegalEntity(recordId)
    }

    @When("the cleaning service refines {string} as a site")
    fun `when refine as site`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service refines '$recordId' as a site" }
        refineAsSite(recordId)
    }

    @When("the cleaning service refines {string} as an additional address of a site")
    fun `when refine as additional address of site`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service refines '$recordId' as an additional address of a site" }
        refineAsAdditionalAddressOfSite(recordId)
    }

    @When("the cleaning service refines {string} as an additional address of a legal entity")
    fun `when refine as additional address of legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service refines '$recordId' as an additional address of a legal entity" }
        refineAsAdditionalAddressOfLegalEntity(recordId)
    }

    @When("a sharing member submits an IsReplacedBy relation {string} from {string}'s address to {string}'s address")
    fun `when submit IsReplacedBy relation`(relationId: String, sourceRecordId: String, targetRecordId: String) {
        logger.info { "[$scenarioName] When: sharing member submits IsReplacedBy relation '$relationId' from '$sourceRecordId' to '$targetRecordId'" }
        val entry = testDataFactoryGate.relation.input.request
            .fromSeed("$relationId${context.scenarioSuffix}")
            .withExternalId(context.runId(relationId))
            .withRelationType(GateRelationType.IsReplacedBy)
            .withSource(context.runId(sourceRecordId))
            .withTarget(context.runId(targetRecordId))
        val request = RelationPutRequest(listOf(entry))
        val response = gateClient.relation.put(true, request)
        attachGateCall("PUT", "/v7/input/relations", request = request, response = response)
        context.relations[relationId] = RelationState(
            submittedEntry = entry,
            sourceRecordId = sourceRecordId,
            targetRecordId = targetRecordId
        )
    }

    // The cleaning service does not reclassify the relation — it confirms the type the
    // sharing member submitted and resolves the BPN references from the Pool addresses.
    @When("the cleaning service accepts relation {string} as submitted")
    fun `when accept relation as submitted`(relationId: String) {
        logger.info { "[$scenarioName] When: cleaning service accepts relation '$relationId' as submitted" }
        val relationState = context.relations[relationId]!!
        val runId = context.runId(relationId)

        sharingStateWatcher.waitForRelationTaskId(relationId)
        val relationSharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(runId), sharingStateTypes = null, updatedAfter = null, paginationRequest = PaginationRequest()
        )
        attachGateCall("GET", "/v7/relations/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = relationSharingStatePage)
        val taskId = relationSharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedRelationTask(taskId)

        // Resolve the task using the actual Pool address BPNs, fetched from Gate output.
        val sourceRunId = context.runId(relationState.sourceRecordId)
        val targetRunId = context.runId(relationState.targetRecordId)
        val sourceOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(sourceRunId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(sourceRunId), response = sourceOutputPage)
        val targetOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(targetRunId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(targetRunId), response = targetOutputPage)
        val sourceBpn = sourceOutputPage.content.single().address.addressBpn
        val targetBpn = targetOutputPage.content.single().address.addressBpn

        // Derive the relation type from what the sharing member submitted — the cleaning
        // service does not override it.
        val submittedRelationType = OrchestratorRelationType.valueOf(relationState.submittedEntry.relationType.name)
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(
            TaskRelationsStepResultRequest(TaskStep.CleanAndSync, listOf(
                TaskRelationsStepResultEntryDto(taskId, BusinessPartnerRelations(
                    relationType = submittedRelationType,
                    businessPartnerSourceBpn = sourceBpn,
                    businessPartnerTargetBpn = targetBpn,
                    validityPeriods = relationState.submittedEntry.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
                    reasonCode = relationState.submittedEntry.reasonCode
                ))
            ))
        )

        // Accepting an IsReplacedBy relation causes address type changes in the Pool.
        // Source (BP1): was the legal address, demoted to additional address.
        // Target (BP2): was an additional address, promoted to the legal address.
        // Address coordinates are unchanged; only the classification changes.
        val sourceState = context.records[relationState.sourceRecordId]!!
        val targetState = context.records[relationState.targetRecordId]!!
        context.records[relationState.sourceRecordId] = sourceState.copy(
            currentExpectedOutput = sourceState.currentExpectedOutput!!.withAddressType(AddressType.AdditionalAddress)
        )
        context.records[relationState.targetRecordId] = targetState.copy(
            currentExpectedOutput = targetState.currentExpectedOutput!!.withAddressType(AddressType.LegalAddress)
        )

        // Store the resolved BPNs for the Then step to use when building its expectation.
        context.relations[relationId] = relationState.copy(
            resolvedSourceBpn = sourceBpn,
            resolvedTargetBpn = targetBpn
        )
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    // Generic for both initial share and update — waits for the most recent success.
    @Then("record {string} reaches sharing success")
    fun `then sharing success`(recordId: String) {
        logger.info { "[$scenarioName] Then: record '$recordId' reaches sharing success" }
        val runId = context.runId(recordId)
        sharingStateWatcher.waitForCompletedState(recordId)
        val sharingStatePage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(runId))
        attachGateCall("GET", "/v7/business-partners/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = sharingStatePage)
        val expected = listOf(SharingStateDto(runId, SharingStateType.Success, updatedAt = Instant.now()))
        assertRepository.assertSharingStates(sharingStatePage.content, expected)
    }

    @Then("{string} output is a legal entity reflecting the refinement and submitted data")
    fun `then output is legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a legal entity reflecting the refinement and submitted data" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("{string} output is a site-based legal entity reflecting the refinement and submitted data")
    fun `then output is site-based legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a site-based legal entity reflecting the refinement and submitted data" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("{string} output is a site reflecting the refinement and submitted data")
    fun `then output is site`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a site reflecting the refinement and submitted data" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("{string} output is an additional address of a site reflecting the refinement and submitted data")
    fun `then output is additional address of site`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is an additional address of a site reflecting the refinement and submitted data" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("{string} output is an additional address of a legal entity reflecting the refinement and submitted data")
    fun `then output is additional address of legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is an additional address of a legal entity reflecting the refinement and submitted data" }
        assertOutputMatchesExpected(recordId)
    }

    // The distinct phrasing signals that both sources (refinement and submission) have
    // changed since the precondition, so the assertion checks the fully replaced state.
    @Then("{string} output is a legal entity reflecting the new refinement and the new submission")
    fun `then output is legal entity with new content`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a legal entity reflecting the new refinement and the new submission" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("relation {string} reaches sharing success")
    fun `then relation sharing success`(relationId: String) {
        logger.info { "[$scenarioName] Then: relation '$relationId' reaches sharing success" }
        val runId = context.runId(relationId)
        sharingStateWatcher.waitForRelationCompletedState(relationId)
        val sharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(runId), sharingStateTypes = null, updatedAfter = null, paginationRequest = PaginationRequest()
        )
        attachGateCall("GET", "/v7/relations/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = sharingStatePage)
        val expected = listOf(RelationSharingStateDto(
            externalId = runId,
            sharingStateType = RelationSharingStateType.Success,
            taskId = sharingStatePage.content.single().taskId,
            updatedAt = Instant.now()
        ))
        assertRepository.assertRelationSharingStates(sharingStatePage.content, expected)
    }

    @Then("relation {string} output is the accepted relation with the two addresses linked")
    fun `then relation output is accepted relation with addresses linked`(relationId: String) {
        logger.info { "[$scenarioName] Then: relation '$relationId' output is the accepted relation with the two addresses linked" }
        val runId = context.runId(relationId)
        val state = context.relations[relationId]!!
        val expectedOutput = testDataFactoryGate.relation.output
            .fromInput(state.submittedEntry)
            .copy(
                externalId = runId,
                sourceBpn = state.resolvedSourceBpn!!,
                targetBpn = state.resolvedTargetBpn!!
            )
        val actual = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(externalIds = listOf(runId)), PaginationRequest()
        )
        attachGateCall("POST", "/v7/output/relations/search", request = RelationOutputSearchRequest(externalIds = listOf(runId)), response = actual)
        assertRepository.assertRelationOutput(actual, PageDto(1, 1, 0, 1, listOf(expectedOutput)))
    }

    @Then("{string} is reclassified to an additional address with its address data unchanged")
    fun `then reclassified to additional address`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' is reclassified to an additional address with its address data unchanged" }
        assertOutputMatchesExpected(recordId)
    }

    @Then("{string} is reclassified to the legal entity's headquarters with its address data unchanged")
    fun `then reclassified to headquarters`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' is reclassified to the legal entity's headquarters with its address data unchanged" }
        assertOutputMatchesExpected(recordId)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    // Stores the uploaded input and content seed in RecordState so the subsequent
    // refine step can derive coherent task data and expected output from the same seed.
    private fun upload(recordId: String, contentSeed: String) {
        val inputData = testDataGenerator.buildInputData(contentSeed).copy(isOwnCompanyData = true)
        val runId = context.runId(recordId)
        val request = listOf(inputData.copy(externalId = runId))
        val response = gateClient.businessParters.upsertBusinessPartnersInput(request)
        attachGateCall("PUT", "/v7/input/business-partners", request = request, response = response.body)
        context.records[recordId] = (context.records[recordId] ?: RecordState()).copy(
            contentSeed = contentSeed,
            currentInput = inputData
        )
    }

    // Waits for the orchestrator to assign a task to the record, reserves it, and
    // resolves it with the provided task data so the pipeline can continue.
    private fun resolveTask(recordId: String, taskData: BusinessPartner) {
        val runId = context.runId(recordId)
        sharingStateWatcher.waitForTaskId(recordId)
        val sharingStatePage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(runId))
        attachGateCall("GET", "/v7/business-partners/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = sharingStatePage)
        val taskId = sharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedTask(taskId)
        orchestratorClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, taskData)))
        )
    }

    // Used by the compound Given to run a full share cycle and block until the
    // sharing state reaches success before handing control back to the scenario.
    private fun completeShare(recordId: String, refine: (String) -> Unit) {
        refine(recordId)
        sharingStateWatcher.waitForCompletedState(recordId)
    }

    // Stores the legal entity so later Given steps can bind an additional address to it.
    private fun refineAsLegalEntity(recordId: String) {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildLegalEntity(state.contentSeed!!)
        resolveTask(recordId, entityResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        context.records[recordId] = state.copy(
            currentTaskData = entityResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromLegalEntity(inputResponse, entityResult.legalEntity),
            legalEntity = entityResult.legalEntity
        )
    }

    // Variant used by the compound Given for the headquarter relocation precondition —
    // binds BP2 to the already-established legal entity from BP1's RecordState rather
    // than generating an independent parent.
    private fun completeShareAsAdditionalAddressOf(
        recordId: String,
        parentLegalEntity: LegalEntityWithLegalAddressVerboseDto
    ) {
        val state = context.records[recordId]!!
        val addressResult = testDataGenerator.buildAdditionalLegalEntityAddress(state.contentSeed!!, parentLegalEntity)
        resolveTask(recordId, addressResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        context.records[recordId] = state.copy(
            currentTaskData = addressResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(
                inputResponse,
                addressResult.additionalLegalEntityAddressWithParent.legalEntity,
                addressResult.additionalLegalEntityAddressWithParent.address
            )
        )
        sharingStateWatcher.waitForCompletedState(recordId)
    }

    private fun refineAsSiteBasedLegalEntity(recordId: String) {
        val state = context.records[recordId]!!
        val entityResult = testDataGenerator.buildSiteBasedLegalEntity(state.contentSeed!!)
        resolveTask(recordId, entityResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        val siteWithMainAddress = SiteWithMainAddressVerboseDto(
            entityResult.siteBasedLegalEntity.site,
            entityResult.siteBasedLegalEntity.legalEntity.legalAddress
        )
        context.records[recordId] = state.copy(
            currentTaskData = entityResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromLegalEntityOnSite(
                inputResponse, entityResult.siteBasedLegalEntity.legalEntity, siteWithMainAddress
            )
        )
    }

    private fun refineAsSite(recordId: String) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        // The parent legal entity is infrastructure for the task resolution, not a
        // separate Gate record. Its seed is derived to keep it structurally distinct.
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent")
        val siteResult = testDataGenerator.buildSite(contentSeed, parentResult.legalEntity)
        resolveTask(recordId, siteResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        context.records[recordId] = state.copy(
            currentTaskData = siteResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromSite(
                inputResponse, siteResult.siteWithParent.legalEntity, siteResult.siteWithParent.site
            )
        )
    }

    private fun refineAsAdditionalAddressOfSite(recordId: String) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent")
        val siteResult = testDataGenerator.buildSite("${contentSeed}Site", parentResult.legalEntity)
        val addressResult = testDataGenerator.buildAdditionalSiteAddress(contentSeed, siteResult.siteWithParent)
        resolveTask(recordId, addressResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        context.records[recordId] = state.copy(
            currentTaskData = addressResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnSite(
                inputResponse,
                addressResult.additionalSiteAddressWithParent.siteWithParent.legalEntity,
                addressResult.additionalSiteAddressWithParent.siteWithParent.site,
                addressResult.additionalSiteAddressWithParent.address
            )
        )
    }

    private fun refineAsAdditionalAddressOfLegalEntity(recordId: String) {
        val state = context.records[recordId]!!
        val contentSeed = state.contentSeed!!
        val parentResult = testDataGenerator.buildLegalEntity("${contentSeed}Parent")
        val addressResult = testDataGenerator.buildAdditionalLegalEntityAddress(contentSeed, parentResult.legalEntity)
        resolveTask(recordId, addressResult.taskData)
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        context.records[recordId] = state.copy(
            currentTaskData = addressResult.taskData,
            currentExpectedOutput = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(
                inputResponse,
                addressResult.additionalLegalEntityAddressWithParent.legalEntity,
                addressResult.additionalLegalEntityAddressWithParent.address
            )
        )
    }

    private fun assertOutputMatchesExpected(recordId: String) {
        val runId = context.runId(recordId)
        val expectedOutput = context.records[recordId]!!.currentExpectedOutput!!.copy(externalId = runId)
        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = output)
        assertRepository.assertBusinessPartnerOutput(output, PageDto(1, 1, 0, 1, listOf(expectedOutput)), assertRepository.outputComparisonConfigNoBpn)
    }

    private fun attachGateCall(method: String, path: String, request: Any? = null, response: Any? = null) {
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
