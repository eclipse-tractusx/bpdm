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
     * Marks a parent, grandparent, or self-owner entity as ownershipUltimate = true in the golden record.
     * The entity must already exist in the Pool.
     */
    @When("the (parent|grandparent|self-owner) entity {string} is marked as ownershipUltimate = true in the golden record")
    @And("the (parent|grandparent|self-owner) entity {string} is marked as ownershipUltimate = true in the golden record")
    fun markEntityAsUltimateOwner(entityType: String, entityId: String) {
        logger.info { "Marking $entityType entity '$entityId' as ownershipUltimate = true" }
        
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
        logger.info { "Successfully marked $entityType entity '$entityId' as ownershipUltimate = true" }
    }

    /**
     * Asserts that a record's output reflects the correct ultimateOwnerBpnl value.
     * The value can be either a record ID (which will be resolved to a BPNL) or null.
     */
    @Then("{string} output reflects ultimateOwnerBpnl as {string}")
    @And("{string} output reflects ultimateOwnerBpnl as {string}")
    fun assertUltimateOwnerBpnl(recordId: String, expectedOwnerRecordId: String) {
        logger.info { "Asserting that '$recordId' output reflects ultimateOwnerBpnl as '$expectedOwnerRecordId'" }
        
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
            .withFailMessage("Expected ultimateOwnerBpnl to be '$expectedBpnl' but was '${output.legalEntity.ultimateOwnerBpnl}'")
            .isEqualTo(expectedBpnl)
        
        logger.info { "Successfully asserted ultimateOwnerBpnl for '$recordId'" }
    }

    /**
     * Asserts that a record's output reflects null as the ultimateOwnerBpnl value.
     */
    @Then("{string} output reflects ultimateOwnerBpnl as null")
    @And("{string} output reflects ultimateOwnerBpnl as null")
    fun assertUltimateOwnerBpnlNull(recordId: String) {
        logger.info { "Asserting that '$recordId' output reflects ultimateOwnerBpnl as null" }
        
        val context = ScenarioContext.current() ?: error("No active scenario context")
        
        // Get the output from the Gate
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        val output = outputPage.content.firstOrNull()
            ?: error("No output found for record '$recordId'")
        
        // Assert the ultimateOwnerBpnl is null
        assertThat(output.legalEntity.ultimateOwnerBpnl)
            .withFailMessage("Expected ultimateOwnerBpnl to be null but was '${output.legalEntity.ultimateOwnerBpnl}'")
            .isNull()
        
        logger.info { "Successfully asserted ultimateOwnerBpnl is null for '$recordId'" }
    }
}
