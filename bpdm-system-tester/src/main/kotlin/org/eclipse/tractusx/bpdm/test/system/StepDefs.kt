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

package org.eclipse.tractusx.bpdm.test.system

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import java.time.Duration

class StepDefs(
    private val gateInputDataFactory: GateInputFactory,
    private val gateOutputFactory: GateOutputFactory,
    private val gateClient: GateClient
): SpringTestRunConfiguration() {

    @When("^the sharing member shares business partner$")
    fun `the sharing member shares business partner`() {
        gateClient.businessParters.upsertBusinessPartnersInput(listOf(gateInputDataFactory.genericFullValidWithSiteWithoutAnyBpn.request))
    }

    @Then("^the sharing member receives legal and site main address output$")
    fun `the sharing member receives legal and site main address output`() {
        val expectedOutput = gateOutputFactory.legalAndSiteMainAddressFullValid

        val result = waitForResult(expectedOutput.externalId)
        assertThat(result).isEqualTo(SharingStateType.Success)

        val actualOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(expectedOutput.externalId), PaginationRequest()).content.single()

        assertThat(actualOutput)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
            .ignoringFields(
                BusinessPartnerOutputDto::createdAt.name,
                BusinessPartnerOutputDto::updatedAt.name,
                "${BusinessPartnerOutputDto::legalEntity.name}.${LegalEntityRepresentationOutputDto::legalEntityBpn.name}",
                "${BusinessPartnerOutputDto::site.name}.${SiteRepresentationOutputDto::siteBpn.name}",
                "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::addressBpn.name}",
                // ToDo: Cleaning service dummy should have fixed confidence criteria dummy times otherwise we need to keep ignoring these fields
                "${BusinessPartnerOutputDto::legalEntity.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
                "${BusinessPartnerOutputDto::legalEntity.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
                "${BusinessPartnerOutputDto::site.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
                "${BusinessPartnerOutputDto::site.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
                "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
                "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
            )
            .isEqualTo(expectedOutput)
    }

    fun waitForResult(externalId: String): SharingStateType {
        println("Waiting for result for $externalId ...")
        return runBlocking {
            withTimeout(Duration.ofMinutes(2)){
                var result: SharingStateType = SharingStateType.Error
                do{
                    val sharingState = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(externalId)).content.single()
                    val isFinished = sharingState.sharingStateType == SharingStateType.Success || sharingState.sharingStateType == SharingStateType.Error
                    if(isFinished) result = sharingState.sharingStateType
                    delay(Duration.ofSeconds(10))
                }while (!isFinished )
                result
            }
        }
    }
}