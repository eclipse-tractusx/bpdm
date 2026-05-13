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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test
import java.time.Instant

class CreateBusinessPartnerInputV7IT: UnscheduledGateTestBaseV7() {

    /**
     * WHEN input manager creates a new empty record
     * THEN empty record returned
     */
    @Test
    fun `create empty business partner input`(){
        //WHEN
        val emptyBusinessPartner= BusinessPartnerInputRequest(externalId = testName)
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(emptyBusinessPartner)).body!!

        //THEN
        val expected = businessPartnerInputFactory.fromRequest(emptyBusinessPartner)
        assertRepo.assertBusinessPartnerInput(response, listOf(expected))
    }

    /**
     * WHEN input manager creates a fully filled business partner record
     * THEN that record returned
     */
    @Test
    fun `create filled business partner input`(){
        //WHEN
        val request =  businessPartnerInputRequestFactory.fromSeed(testName)
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!

        //THEN
        val expected = businessPartnerInputFactory.fromRequest(request)
        assertRepo.assertBusinessPartnerInput(response, listOf(expected))
    }

    /**
     * GIVEN business partner input without external sequence timestamp
     * WHEN input manager updates the business partner input with new data
     * THEN response contains updated business partner input data
     */
    @Test
    fun `update business partner input`() {
        //GIVEN
        testDataClient.upsertBusinessPartnerInput(
            businessPartnerInputRequestFactory.fromSeed("Initial $testName")
                .copy(externalId = testName, externalSequenceTimestamp = null)
        )

        //WHEN
        val request = businessPartnerInputRequestFactory.fromSeed("Updated $testName").copy(externalId = testName)
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!

        //THEN
        val expected = businessPartnerInputFactory.fromRequest(request)
        assertRepo.assertBusinessPartnerInput(response, listOf(expected))
    }

    /**
     * GIVEN business partner input
     * WHEN input manager updates the business partner input with new data with a newer external sequence timestamp
     * THEN response contains updated business partner input data
     */
    @Test
    fun `update business partner input with newer sequence timestamp`() {
        //GIVEN
        val created = testDataClient.upsertBusinessPartnerInput(
            businessPartnerInputRequestFactory.fromSeed("Initial $testName").copy(externalId = testName)
        )

        //WHEN
        val request = businessPartnerInputRequestFactory.fromSeed("Updated $testName")
            .copy(externalId = testName, externalSequenceTimestamp = created.externalSequenceTimestamp!!.plusSeconds(10))
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!

        //THEN
        val expected = businessPartnerInputFactory.fromRequest(request)
        assertRepo.assertBusinessPartnerInput(response, listOf(expected))
    }


    /**
     * GIVEN business partner input
     * WHEN input manager updates the business partner input with the same data
     * THEN response does not contain business partner input (as it did not change)
     */
    @Test
    fun `try update input with no changes`() {
        //GIVEN
        testDataClient.upsertBusinessPartnerInput(testName)

        //WHEN
        val request = businessPartnerInputRequestFactory.fromSeed(testName)
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body

        //THEN
        Assertions.assertThat(response).isEmpty()
    }

    /**
     * GIVEN business partner input with external sequence timestamp
     * WHEN input manager updates the business partner input with new values but an older timestamp
     * THEN response does not contain business partner input (as update was not taken)
     */
    @Test
    fun `try update input with earlier external sequence timestamp`() {
        //GIVEN
        val created = testDataClient.upsertBusinessPartnerInput(
            businessPartnerInputRequestFactory.fromSeed("Initial $testName")
                .copy(externalId = testName, externalSequenceTimestamp = Instant.now())
        )

        //WHEN
        val request = businessPartnerInputRequestFactory.fromSeed("Updated $testName")
            .copy(externalId = testName, externalSequenceTimestamp = created.externalSequenceTimestamp!!.minusSeconds(10))
        val response = gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body

        //THEN
        Assertions.assertThat(response).isEmpty()
    }
}