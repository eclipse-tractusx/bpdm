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
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import tools.jackson.databind.json.JsonMapper

/**
 * Steps for the "Ultimate Owner Distribution" feature.
 *
 * These steps handle marking legal entities as ultimate owners and verifying that the
 * ultimateOwnerBpnl is correctly reflected in the output of owned entities.
 */
class UltimateOwnerDistributionStepDefs(
    private val gateClient: GateClient,
    private val poolClient: PoolApiClient,
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
        
        // Get the legal entity from context
        val legalEntityWithAddress = context.legalEntities[entityId] 
            ?: error("Legal entity '$entityId' not found in scenario context")
        
        // Create the Gate input representation with ultimate owner flag
        val legalEntityInput = LegalEntityRepresentationInputDto(
            legalEntityBpn = legalEntityWithAddress.header.bpnl,
            legalName = legalEntityWithAddress.header.legalName,
            shortName = legalEntityWithAddress.header.legalShortName,
            legalForm = legalEntityWithAddress.header.legalFormVerbose?.technicalKey,
            ownershipUltimate = true,
            states = emptyList()
        )
        
        // Upload to Gate via upsertBusinessPartnersInput
        val inputRequest = context.inputData[entityId]?.copy(
            externalId = context.runId(entityId),
            legalEntity = legalEntityInput
        ) ?: error("Input data for '$entityId' not found in scenario context")
        
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputRequest))
        logger.info { "Successfully uploaded '$entityId' as ultimate owner to Gate" }
    }

    /**
     * Golden record process confirms an entity as the ultimate owner.
     * This updates the Pool with the ultimate owner flag from the golden record.
     */
    @And("the golden record process confirms {string} as the ultimate owner")
    fun goldenRecordConfirmsAsUltimateOwner(entityId: String) {
        logger.info { "Golden record process confirms '$entityId' as the ultimate owner" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        
        // Get the legal entity from context
        val legalEntityWithAddress = context.legalEntities[entityId] 
            ?: error("Legal entity '$entityId' not found in scenario context")
        
        // Convert verbose DTO to non-verbose DTO using JSON serialization
        val verboseJson = jsonMapper.writeValueAsString(legalEntityWithAddress)
        val legalEntity = jsonMapper.readValue(verboseJson, LegalEntityDto::class.java)
        
        // Update the legal entity to mark it as ultimate owner
        val updatedLegalEntity = legalEntity.copy(
            header = legalEntity.header.copy(
                ownershipUltimate = true
            )
        )
        
        // Update the legal entity in the Pool to mark it as ultimate owner
        val updateRequest = LegalEntityPartnerUpdateRequest(
            bpnl = legalEntityWithAddress.header.bpnl,
            legalEntity = updatedLegalEntity
        )
        
        poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))
        logger.info { "Successfully confirmed '$entityId' as ultimate owner in Pool" }
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
