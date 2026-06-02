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
import io.cucumber.java.BeforeStep
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateAssertRepositoryV7
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerRelationTestDataGenerator
import org.eclipse.tractusx.bpdm.test.system.utils.RelationOutputContext
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.StepUtils
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TestRepository
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestRunData
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.gate.withoutAnyBpn
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType as PoolRelationType


class BusinessPartnerRelationStepDefs(
    private val testRunData: TestRunData,
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val testDataGenerator: BusinessPartnerRelationTestDataGenerator,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val assertRepository: GateAssertRepositoryV7
): SpringTestRunConfiguration() {

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

    @Given("relation input data {string} of type {string} from {string} to {string}")
    fun `given relation input data`(relationInputDataId: String, relationType: String, fromRecordId: String, toRecordId: String) {
        logger.info { "[$scenarioName] Given: relation input data '$relationInputDataId' of type '$relationType' from '$fromRecordId' to '$toRecordId'" }
        val result = testDataGenerator.buildRelationInputData(relationInputDataId, relationType, fromRecordId, toRecordId)
        context.relationInputData[relationInputDataId] = result.relationInputEntry
    }

    @Given("relation output data {string} based on input {string}")
    fun `given relation output data`(relationOutputDataId: String, relationInputDataId: String) {
        logger.info { "[$scenarioName] Given: relation output data '$relationOutputDataId' based on input '$relationInputDataId'" }
        val inputEntry = context.relationInputData[relationInputDataId]!!
        val outputDto = testDataFactoryGate.relation.output.fromInput(inputEntry)
        context.relationOutputData[relationOutputDataId] = RelationOutputContext(
            outputDto = outputDto,
            sourceExternalId = inputEntry.businessPartnerSourceExternalId,
            targetExternalId = inputEntry.businessPartnerTargetExternalId
        )
    }

    @When("uploading into relation record {string} input data {string}")
    fun `when uploading into relation record input data`(recordId: String, inputDataId: String) {
        logger.info { "[$scenarioName] When: uploading into relation record '$recordId' input data '$inputDataId'" }
        val inputEntry = context.relationInputData[inputDataId]!!
        val request = RelationPutRequest(listOf(inputEntry.copy(externalId = context.runId(recordId))))
        gateClient.relation.put(true, request)
    }

    @When("relation record {string} is refined to {string}")
    fun `when relation record is refined to`(recordId: String, relationOutputDataId: String) {
        logger.info { "[$scenarioName] When: relation record '$recordId' is refined to '$relationOutputDataId'" }
        val recordRunId = context.runId(recordId)
        val relationOutput = context.relationOutputData[relationOutputDataId]!!

        sharingStateWatcher.waitForRelationTaskId(recordId)

        val sharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(recordRunId),
            sharingStateTypes = null,
            updatedAfter = null,
            paginationRequest = PaginationRequest()
        )
        val taskId = sharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedRelationTask(taskId)

        val sourceOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(relationOutput.sourceExternalId)).content.single()
        val targetOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(relationOutput.targetExternalId)).content.single()

        val relationOutputWithBPNs = relationOutput.outputDto.copy(
            sourceBpn = sourceOutput.legalEntity.legalEntityBpn,
            targetBpn = targetOutput.legalEntity.legalEntityBpn
        )
        context.relationOutputData[relationOutputDataId] = relationOutput.copy(outputDto = relationOutputWithBPNs)

        val refinedRelation = BusinessPartnerRelations(
            relationType = RelationType.valueOf(relationOutputWithBPNs.relationType.name),
            businessPartnerSourceBpn = relationOutputWithBPNs.sourceBpn,
            businessPartnerTargetBpn = relationOutputWithBPNs.targetBpn,
            validityPeriods = relationOutputWithBPNs.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = relationOutputWithBPNs.reasonCode
        )

        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(
            TaskRelationsStepResultRequest(TaskStep.CleanAndSync, listOf(TaskRelationsStepResultEntryDto(taskId, refinedRelation)))
        )
    }

    @Then("polling relation record {string} sharing state leads to success")
    fun `then polling relation record sharing state leads to success`(recordId: String) {
        logger.info { "[$scenarioName] Then: polling relation record '$recordId' sharing state leads to success" }
        val recordRunId = context.runId(recordId)

        sharingStateWatcher.waitForRelationCompletedState(recordId)

        val sharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(recordRunId),
            sharingStateTypes = null,
            updatedAfter = null,
            paginationRequest = PaginationRequest()
        )
        val expectedSharingStates = listOf(
            RelationSharingStateDto(
                externalId = recordRunId,
                sharingStateType = RelationSharingStateType.Success,
                taskId = sharingStatePage.content.single().taskId,
                updatedAt = Instant.now()
            )
        )
        assertRepository.assertRelationSharingStates(sharingStatePage.content, expectedSharingStates)
    }

    @Then("relation record {string} output data matches {string}")
    fun `then relation record output data matches`(recordId: String, outputDataId: String) {
        logger.info { "[$scenarioName] Then: relation record '$recordId' output data matches '$outputDataId'" }
        val recordRunId = context.runId(recordId)
        val expectedOutputDto = context.relationOutputData[outputDataId]!!.outputDto.copy(externalId = recordRunId)

        val actual = gateClient.relationOutput.postSearch(
            RelationOutputSearchRequest(externalIds = listOf(recordRunId)),
            PaginationRequest()
        )
        assertRepository.assertRelationOutput(actual, PageDto(1, 1, 0, 1, listOf(expectedOutputDto)))
    }

}