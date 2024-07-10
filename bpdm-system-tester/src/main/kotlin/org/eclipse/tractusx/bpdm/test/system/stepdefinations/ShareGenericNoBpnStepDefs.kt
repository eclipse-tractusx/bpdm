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
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.test.system.SpringTestRunConfiguration
import org.eclipse.tractusx.bpdm.test.system.config.TestRunData
import org.eclipse.tractusx.bpdm.test.system.utils.*

class ShareGenericNoBpnStepDefs(
    private val gateInputDataFactory: GateInputFactory,
    private val gateOutputFactory: GateOutputFactory,
    private val gateClient: GateClient,
    private val stepUtils: StepUtils,
    private val testRunData: TestRunData
): SpringTestRunConfiguration() {

    @Given("^output \"([^\"]*)\" with external-ID \"([^\"]*)\"$")
    fun `given output seed with externalId`(seed: String, externalId: String) {
        uploadInput(seed, externalId, null)
        stepUtils.waitForResult(testRunData.toExternalId(externalId))
    }

    @When("^the sharing member uploads full valid input \"([^\"]*)\" with external-ID \"([^\"]*)\" with address type \"([^\"]*)\"$")
    fun `when the sharing member uploads input seed with address type`(seed: String, externalId: String, addressType: String) {
        uploadInput(seed, externalId, AddressType.valueOf(addressType))
    }

    @When("^the sharing member uploads full valid input \"([^\"]*)\" with external-ID \"([^\"]*)\" without address type")
    fun `when the sharing member uploads input seed without address type`(seed: String, externalId: String) {
        uploadInput(seed, externalId, null)
    }

    @Then("^the sharing member receives output \"([^\"]*)\" with external-ID \"([^\"]*)\" with address type \"([^\"]*)\"$")
    fun `then the sharing member receives output seed with modifier`(seed: String, externalId: String, addressType: String) {
        val expectedOutput = gateOutputFactory.createOutput(seed, externalId)
            .withAddressType(AddressType.valueOf(addressType))
            // On no auth config we can't get own company data to true
            .copy(isOwnCompanyData = false)

        stepUtils.waitForResult(expectedOutput.externalId)

        val actualOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(expectedOutput.externalId), PaginationRequest()).content.single()

        stepUtils.assertEqualIgnoreBpns(actualOutput, expectedOutput)
    }


    private fun uploadInput(seed: String, externalId: String, addressType: AddressType?){
        val inputRequest =  gateInputDataFactory.createFullValid(seed, externalId).withAddressType(addressType).withoutAnyBpn()
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(inputRequest))
    }


}