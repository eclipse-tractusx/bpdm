/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.test.system.utils.GateInputFactory
import org.eclipse.tractusx.bpdm.test.system.SpringTestRunConfiguration
import java.time.Duration

class CreateUpdateBusinessPartnerStepDefs(
    private val gateInputDataFactory: GateInputFactory,
    private val gateClient: GateClient
): SpringTestRunConfiguration() {

    private lateinit var inputRequest: BusinessPartnerInputRequest
    private lateinit var createdOutput: BusinessPartnerOutputDto
    private lateinit var updatedOutput: BusinessPartnerOutputDto

    // Step 1: Creating the business partner
    @Given("^the sharing member provides valid business partner input$")
    fun `the sharing member provides valid business partner input`() {
        inputRequest = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request
    }

    @When("^the sharing member creates the business partner$")
    fun `the sharing member creates the business partner`() {
        // Send request to create the business partner
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputRequest))

        // Wait for the result to be processed
        val result = waitForResult(inputRequest.externalId)
        assertThat(result).isEqualTo(SharingStateType.Success)

        // Fetch the created business partner's output
        createdOutput = gateClient.businessParters
            .getBusinessPartnersOutput(listOf(inputRequest.externalId), PaginationRequest()).content.single()

        println("Created business partner output... $createdOutput")
    }

    @Then("^the sharing member should receive a BPN for the business partner$")
    fun `the sharing member should receive a BPN for the business partner`() {
        // Assert that a BPN is assigned in the created output
        assertThat(createdOutput.legalEntity.legalEntityBpn).isNotNull()
        println("Assigned BPN: ${createdOutput.legalEntity.legalEntityBpn}")
    }

    // Step 2: Updating the business partner data
    @Given("^the sharing member has created a business partner$")
    fun `the sharing member has created a business partner`() {
        // Check if createdOutput is already initialized from a previous step
        if (!::createdOutput.isInitialized) {
            `the sharing member provides valid business partner input`()
            `the sharing member creates the business partner`()
        }
        assertThat(::createdOutput.isInitialized).isTrue()
    }

    @And("^the sharing member provides updated data for the business partner$")
    fun `the sharing member provides updated data for the business partner`() {
        // Update only the short name (or any other specific field) in the legal entity
        val updatedRequest = inputRequest.copy(
            legalEntity = inputRequest.legalEntity.copy(shortName = "Updated Short Name")
        )

        // Send request to update the business partner
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(updatedRequest))

        // Wait for the result to be processed
        val result = waitForResult(updatedRequest.externalId)
        assertThat(result).isEqualTo(SharingStateType.Success)
    }

    @When("^the sharing member updates the business partner$")
    fun `the sharing member updates the business partner`() {
        // Fetch the updated business partner's output
        updatedOutput = gateClient.businessParters
            .getBusinessPartnersOutput(listOf(inputRequest.externalId), PaginationRequest()).content.single()

        println("Updated business partner output... $updatedOutput")
    }

    @Then("^the sharing member should receive updated business partner data with the same BPN$")
    fun `the sharing member should receive updated business partner data with the same BPN`() {
        // Assert that the BPN is the same as before
        assertThat(updatedOutput.legalEntity.legalEntityBpn).isEqualTo(createdOutput.legalEntity.legalEntityBpn)
    }

    @And("^the updated data should reflect the changes$")
    fun `the updated data should reflect the changes`() {
        // Assert that the short name has been updated
        assertThat(updatedOutput.legalEntity.shortName).isEqualTo("Updated Short Name")
        println("Updated short name: ${updatedOutput.legalEntity.shortName}")
    }

    fun waitForResult(externalId: String): SharingStateType = runBlocking {
        println("Waiting for result for $externalId ...")
        withTimeout(Duration.ofMinutes(3)) {
            while (true) {
                val sharingState = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(externalId)).content.single()
                if (sharingState.sharingStateType == SharingStateType.Success || sharingState.sharingStateType == SharingStateType.Error) {
                    return@withTimeout sharingState.sharingStateType
                }
                delay(Duration.ofSeconds(10))
            }
        } as SharingStateType
    }
}