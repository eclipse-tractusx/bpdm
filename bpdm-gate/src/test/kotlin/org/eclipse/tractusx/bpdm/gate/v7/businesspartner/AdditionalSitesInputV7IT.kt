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

package org.eclipse.tractusx.bpdm.gate.v7.businesspartner

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.tractusx.bpdm.gate.api.model.response.AdditionalSiteInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class AdditionalSitesInputV7IT : UnscheduledGateTestBaseV7() {

    /**
     * WHEN input manager creates a record stating further sites of its address
     * THEN that record is returned with those sites
     */
    @Test
    fun `create business partner input with additional sites`() {
        //WHEN
        val request = testData.businessPartner.input.request.fromSeed(testName).copy(additionalSites = additionalSites())
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!

        //THEN
        val expected = testData.businessPartner.input.response.fromRequest(request)
        assertRepo.assertBusinessPartnerInput(response, listOf(expected))
    }

    /**
     * GIVEN a record stating further sites of its address, one known by BPNS and one only by name
     * WHEN the record is shared
     * THEN the golden record process receives both, the known one as a BPN and the other to be resolved by its name
     */
    @Test
    fun `share additional sites with the golden record process`() {
        //GIVEN
        val request = testData.businessPartner.input.request.fromSeed(testName).copy(additionalSites = additionalSites())
        testDataClient.businessPartner.upsertInput(request)

        //WHEN
        val sharedBusinessPartner = testDataClient.businessPartner.sharedBusinessPartnerOf(request.externalId)

        //THEN
        val sharedByBpn = sharedBusinessPartner.additionalSites.single { it.bpnReference.referenceType == BpnReferenceType.Bpn }
        val sharedByName = sharedBusinessPartner.additionalSites.single { it.bpnReference.referenceType == null }

        assertThat(sharedByBpn.bpnReference.referenceValue).isEqualTo("BPNS0000000042XY")
        assertThat(sharedByBpn.siteName).isEqualTo("Known Additional Site $testName")
        assertThat(sharedByName.bpnReference.referenceValue).isNull()
        assertThat(sharedByName.siteName).isEqualTo("Unknown Additional Site $testName")
    }

    /**
     * WHEN input manager creates a record stating further sites of its address but no site of its own
     * THEN the record is rejected as a bad request
     */
    @Test
    fun `try to create business partner input with additional sites but no site`() {
        //WHEN
        val request = testData.businessPartner.input.request.fromSeed(testName).copy(
            site = SiteRepresentationInputDto(),
            additionalSites = additionalSites()
        )

        //THEN
        assertThatThrownBy { gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun additionalSites() = listOf(
        AdditionalSiteInputDto(siteBpn = "BPNS0000000042XY", name = "Known Additional Site $testName"),
        AdditionalSiteInputDto(siteBpn = null, name = "Unknown Additional Site $testName")
    )
}
