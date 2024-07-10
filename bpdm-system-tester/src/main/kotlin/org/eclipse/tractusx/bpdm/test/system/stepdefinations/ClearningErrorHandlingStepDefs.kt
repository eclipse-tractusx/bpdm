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
import org.eclipse.tractusx.bpdm.test.system.utils.GateInputFactory
import org.eclipse.tractusx.bpdm.test.system.SpringTestRunConfiguration
import java.time.Duration

class ClearningErrorHandlingStepDefs(
    private val gateInputDataFactory: GateInputFactory,
    private val gateClient: GateClient
): SpringTestRunConfiguration() {

    private lateinit var inputRequest: BusinessPartnerInputRequest
    private lateinit var result: SharingStateType

    @Given("^the sharing member provides invalid business partner input with missing \"([^\"]*)\"$")
    fun `the sharing member provides invalid business partner input`(errorField: String) {
        // Prepare invalid input by removing the necessary field to simulate the error
        inputRequest = when (errorField) {
            "legalName" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                legalEntity = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.legalEntity.copy(legalName = null)
            )
            "physicalAddress.country" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    physicalPostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.physicalPostalAddress.copy(country = null)
                )
            )
            "physicalAddress.city" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    physicalPostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.physicalPostalAddress.copy(city = null)
                )
            )
            "alternativeAddress.country" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    alternativePostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.alternativePostalAddress!!.copy(country = null)
                )
            )
            "alternativeAddress.city" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    alternativePostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.alternativePostalAddress!!.copy(city = null)
                )
            )
            "alternativeAddress.deliveryServiceType" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    alternativePostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.alternativePostalAddress!!.copy(deliveryServiceType = null)
                )
            )
            "alternativeAddress.deliveryServiceNumber" -> gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.copy(
                address = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.copy(
                    alternativePostalAddress = gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request.address.alternativePostalAddress!!.copy(deliveryServiceNumber = null)
                )
            )
            // Similarly we can handle other fields...
            else -> throw IllegalArgumentException("Unsupported error field: $errorField")
        }
    }

    @When("^the sharing member attempts to create the business partner$")
    fun `the sharing member attempts to create the business partner`() {
        // Attempt to create the business partner and catch any errors
        result = try {
            gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputRequest))
            waitForResult(inputRequest.externalId)
        } catch (e: Exception) {
            SharingStateType.Error
        }
    }

    @Then("^the sharing member should receive an error message \"([^\"]*)\"$")
    fun `the sharing member should receive an error message`(expectedErrorMessage: String) {
        // Validate that the error message received is as expected
        assertThat(result).isEqualTo(SharingStateType.Error)

        val sharingState = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(inputRequest.externalId)).content.single()
        assertThat(sharingState.sharingErrorMessage).contains(expectedErrorMessage)
        println("Error message received: ${sharingState.sharingErrorMessage}")
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