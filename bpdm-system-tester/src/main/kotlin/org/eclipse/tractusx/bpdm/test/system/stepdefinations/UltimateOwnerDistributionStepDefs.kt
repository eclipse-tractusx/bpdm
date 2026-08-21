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

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import tools.jackson.databind.json.JsonMapper

/**
 * Steps for the "Ultimate Owner Distribution" feature.
 *
 * These steps handle marking legal entities as ultimate owners and verifying that the
 * ultimateOwnerBpnl is correctly reflected in the output of owned entities.
 */
class UltimateOwnerDistributionStepDefs(
    private val orchestratorClient: OrchestrationApiClient,
    private val gateClient: GateClient,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val sharingStateWatcher: SharingStateWatcher,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Sharing member marks an entity as the ultimate owner via the Gate.
     * This uploads the ultimate owner flag to the Gate input.
     */
    @When("the sharing member marks {string} as the ultimate owner")
    fun sharingMemberMarksAsUltimateOwner(entityId: String) {
        logger.info { "Sharing member marks '$entityId' as the ultimate owner" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        
        // Get the input data from context and update it with ultimate owner flag
        val inputData = context.records[entityId]?.currentInput
            ?: error("No shared input found for entity '$entityId'")
        
        // Create updated input with ultimate owner flag
        val updatedLegalEntity = inputData.legalEntity.copy(ownershipUltimate = true)
        val updatedInput = inputData.copy(
            externalId = context.runId(entityId),
            legalEntity = updatedLegalEntity
        )
        
        // Upload to Gate via upsertBusinessPartnersInput
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(updatedInput))
        logger.info { "Successfully uploaded '$entityId' as ultimate owner to Gate" }
    }

    /**
     * Golden record process confirms an entity as the ultimate owner.
     * This triggers the golden record processing through the orchestrator.
     */
    @And("the golden record process confirms {string} as the ultimate owner")
    fun goldenRecordConfirmsAsUltimateOwner(entityId: String) {
        logger.info { "Golden record process confirms '$entityId' as the ultimate owner" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        val recordRunId = context.runId(entityId)
        
        // Wait for the golden record task to be created
        sharingStateWatcher.waitForTaskId(entityId)
        
        // Get the sharing state to retrieve the task ID
        val sharingStatesPage = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(recordRunId))
        val taskId = sharingStatesPage.content.singleOrNull()?.taskId
            ?: error("No task ID found for entity '$entityId'")
        
        // Wait for the task to be reserved
        val reservedTask = taskReservationWatcher.waitForReservedTask(taskId)
        
        // Resolve the task to trigger the golden record processing
        orchestratorClient.goldenRecordTasks.resolveStepResults(
            TaskStepResultRequest(TaskStep.CleanAndSync, listOf(TaskStepResultEntryDto(taskId, reservedTask.businessPartner)))
        )
        
        logger.info { "Successfully triggered golden record processing for '$entityId'" }
    }

    /**
     * Asserts that a record's output reflects a specific entity as the ultimate owner.
     */
    @Then("{string} output reflects {string} as the ultimate owner")
    fun assertUltimateOwner(recordId: String, expectedOwnerRecordId: String) {
        logger.info { "Asserting that '$recordId' output reflects '$expectedOwnerRecordId' as the ultimate owner" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        
        // Get the output from the Gate
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        val output = outputPage.content.firstOrNull()
            ?: error("No output found for record '$recordId'")
        
        // Get the expected owner's BPNL
        val expectedOwnerRunId = context.runId(expectedOwnerRecordId)
        val ownerOutputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(expectedOwnerRunId))
        val ownerOutput = ownerOutputPage.content.firstOrNull()
            ?: error("No output found for owner record '$expectedOwnerRecordId'")
        
        val expectedBpnl = ownerOutput.legalEntity.legalEntityBpn
        
        // Assert the ultimateOwnerBpnl matches
        assertThat(output.legalEntity.ultimateOwnerBpnl)
            .withFailMessage("Expected '$recordId' to have ultimate owner '$expectedBpnl' but was '${output.legalEntity.ultimateOwnerBpnl}'")
            .isEqualTo(expectedBpnl)
        
        logger.info { "Successfully asserted '$recordId' has ultimate owner '$expectedOwnerRecordId'" }
    }

    /**
     * Asserts that a record's output has no ultimate owner (null).
     */
    @Then("{string} output has no ultimate owner")
    fun assertNoUltimateOwner(recordId: String) {
        logger.info { "Asserting that '$recordId' output has no ultimate owner" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        
        // Get the output from the Gate
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        val output = outputPage.content.firstOrNull()
            ?: error("No output found for record '$recordId'")
        
        // Assert the ultimateOwnerBpnl is null
        assertThat(output.legalEntity.ultimateOwnerBpnl)
            .withFailMessage("Expected '$recordId' to have no ultimate owner but was '${output.legalEntity.ultimateOwnerBpnl}'")
            .isNull()
        
        logger.info { "Successfully asserted '$recordId' has no ultimate owner" }
    }
}
