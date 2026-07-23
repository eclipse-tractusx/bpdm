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

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.time.LocalDate
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.RecordState
import org.eclipse.tractusx.bpdm.test.system.utils.RelationState
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withExternalId
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withValidityPeriods
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class ShareOwnCompanyPartnersStepDefs(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val shareActions: BusinessPartnerShareActions,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertRepository: GateAssertRepositoryV7,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("{string} has already been shared as a legal entity")
    fun `given already shared as legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Given: '$recordId' has already been shared as a legal entity" }
        shareActions.upload(recordId, isOwnCompanyData = true)
        shareActions.completeShare(recordId) { shareActions.refineAsLegalEntity(it, verified = false) }
    }

    @Given("{string} has already been shared as an additional address of {string}'s legal entity")
    fun `given already shared as additional address of legal entity`(addressRecordId: String, legalEntityRecordId: String) {
        logger.info { "[$scenarioName] Given: '$addressRecordId' has already been shared as an additional address of '$legalEntityRecordId''s legal entity" }
        val parentLegalEntity = context.records[legalEntityRecordId]?.legalEntity
            ?: error("'$legalEntityRecordId' must appear in an earlier Given step and be refined as a legal entity")
        shareActions.upload(addressRecordId, isOwnCompanyData = true)
        shareActions.completeShareAsAdditionalAddressOf(addressRecordId, parentLegalEntity)
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("a sharing member uploads their own company data as record {string}")
    fun `when upload own company data`(recordId: String) {
        logger.info { "[$scenarioName] When: sharing member uploads own company data as record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = true)
    }

    @When("a sharing member re-uploads {string} with different content")
    fun `when reupload with different content`(recordId: String) {
        logger.info { "[$scenarioName] When: sharing member re-uploads '$recordId' with different content" }
        shareActions.upload(recordId, isOwnCompanyData = true, contentSeed = "${recordId}Updated")
    }

    @When("the cleaning service provider refines {string} as a legal entity")
    fun `when cleaning service provider refines as legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a legal entity" }
        shareActions.refineAsLegalEntity(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as a site-based legal entity")
    fun `when cleaning service provider refines as site-based legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a site-based legal entity" }
        shareActions.refineAsSiteBasedLegalEntity(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as a site")
    fun `when cleaning service provider refines as site`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a site" }
        shareActions.refineAsSite(recordId)
    }

    @When("the cleaning service provider refines {string} as an additional address of a site")
    fun `when cleaning service provider refines as additional address of site`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as an additional address of a site" }
        shareActions.refineAsAdditionalAddressOfSite(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as an additional address of a legal entity")
    fun `when cleaning service provider refines as additional address of legal entity`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as an additional address of a legal entity" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, verified = false)
    }

    @When("a sharing member submits relation {string}:")
    fun `when submit relation`(relationId: String, dataTable: DataTable) {
        val data = dataTable.asMap()
        val relationType = GateRelationType.valueOf(data["type"]!!)
        val sourceRecordId = data["from"]!!
        val targetRecordId = data["to"]!!
        logger.info { "[$scenarioName] When: sharing member submits $relationType relation '$relationId' from '$sourceRecordId' to '$targetRecordId'" }
        val baseEntry = testDataFactoryGate.relation.input.request
            .fromSeed("$relationId${context.scenarioSuffix}")
            .withExternalId(context.runId(relationId))
            .withRelationType(relationType)
            .withSource(context.runId(sourceRecordId))
            .withTarget(context.runId(targetRecordId))
        val entry = if (data["validity"] == "currently valid")
            baseEntry.withValidityPeriods(listOf(RelationValidityPeriodDto(validFrom = LocalDate.now(), validTo = null)))
        else
            baseEntry
        val request = RelationPutRequest(listOf(entry))
        val response = gateClient.relation.put(true, request)
        attachGateCall("PUT", "/v7/input/relations", request = request, response = response)
        context.relations[relationId] = RelationState(
            submittedEntry = entry,
            sourceRecordId = sourceRecordId,
            targetRecordId = targetRecordId
        )
    }

    @When("the cleaning service provider accepts relation {string} as submitted")
    fun `when cleaning service provider accepts relation as submitted`(relationId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider accepts relation '$relationId' as submitted" }
        val relationState = context.relations[relationId]!!
        val runId = context.runId(relationId)

        sharingStateWatcher.waitForRelationTaskId(relationId)
        val relationSharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(runId), sharingStateTypes = null, updatedAfter = null, paginationRequest = PaginationRequest()
        )
        attachGateCall("GET", "/v7/relations/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = relationSharingStatePage)
        val taskId = relationSharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedRelationTask(taskId)

        val sourceRunId = context.runId(relationState.sourceRecordId)
        val targetRunId = context.runId(relationState.targetRecordId)
        val sourceOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(sourceRunId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(sourceRunId), response = sourceOutputPage)
        val targetOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(targetRunId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(targetRunId), response = targetOutputPage)
        val sourceBpn = sourceOutputPage.content.single().address.addressBpn
        val targetBpn = targetOutputPage.content.single().address.addressBpn

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

        context.relations[relationId] = relationState.copy(
            resolvedSourceBpn = sourceBpn,
            resolvedTargetBpn = targetBpn
        )
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

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
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromLegalEntity(inputResponse, state.legalEntity!!))
    }

    @Then("{string} output is a site-based legal entity reflecting the refinement and submitted data")
    fun `then output is site-based legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a site-based legal entity reflecting the refinement and submitted data" }
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromLegalEntityOnSite(inputResponse, state.legalEntity!!, state.poolSite!!))
    }

    @Then("{string} output is a site reflecting the refinement and submitted data")
    fun `then output is site`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a site reflecting the refinement and submitted data" }
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromSite(inputResponse, state.legalEntity!!, state.poolSite!!))
    }

    @Then("{string} output is an additional address of a site reflecting the refinement and submitted data")
    fun `then output is additional address of site`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is an additional address of a site reflecting the refinement and submitted data" }
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnSite(inputResponse, state.legalEntity!!, state.poolSite!!, state.poolAddress!!))
    }

    @Then("{string} output is an additional address of a legal entity reflecting the refinement and submitted data")
    fun `then output is additional address of legal entity`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is an additional address of a legal entity reflecting the refinement and submitted data" }
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(inputResponse, state.legalEntity!!, state.poolAddress!!))
    }

    @Then("{string} output is a legal entity reflecting the new refinement and the new submission")
    fun `then output is legal entity with new content`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output is a legal entity reflecting the new refinement and the new submission" }
        val state = context.records[recordId]!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        assertOutput(recordId, testDataFactoryGate.businessPartner.output.fromLegalEntity(inputResponse, state.legalEntity!!))
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

    @Then("{string} is reclassified to an additional address")
    fun `then reclassified to additional address`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' is reclassified to an additional address" }
        val state = context.records[recordId]!!
        val legalEntity = state.legalEntity!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        val base = testDataFactoryGate.businessPartner.output.fromLegalEntity(inputResponse, legalEntity)
        val expected = base.copy(
            identifiers = legalEntity.legalAddress.identifiers.map { BusinessPartnerIdentifierDto(it.type, it.value, null) },
            states = legalEntity.legalAddress.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            address = base.address.copy(addressType = AddressType.AdditionalAddress)
        )
        assertOutput(recordId, expected)
    }

    @Then("{string} is reclassified to the legal entity's headquarters")
    fun `then reclassified to headquarters`(recordId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' is reclassified to the legal entity's headquarters" }
        val state = context.records[recordId]!!
        val legalEntity = state.legalEntity!!
        val inputResponse = testDataFactoryGate.businessPartner.input.response.fromRequest(state.currentInput!!)
        val base = testDataFactoryGate.businessPartner.output.fromAdditionalAddressOnLegalEntity(inputResponse, legalEntity, state.poolAddress!!)
        val expected = base.copy(
            identifiers = legalEntity.header.identifiers.map { BusinessPartnerIdentifierDto(it.type, it.value, it.issuingBody) },
            states = legalEntity.header.states.map { BusinessPartnerStateDto(it.validFrom, it.validTo, it.type) },
            address = base.address.copy(addressType = AddressType.LegalAddress)
        )
        assertOutput(recordId, expected)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun assertOutput(recordId: String, expectedOutput: BusinessPartnerOutputDto) {
        val runId = context.runId(recordId)
        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = output)
        assertRepository.assertBusinessPartnerOutput(output, PageDto(1, 1, 0, 1, listOf(expectedOutput.copy(externalId = runId))), assertRepository.outputComparisonConfigNoBpn)
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
