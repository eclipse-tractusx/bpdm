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

package org.eclipse.tractusx.bpdm.gate.v6.inputconsumer

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsAddressTypesResponse
import org.junit.jupiter.api.Test

class CountAddressTypesV6IT: InputConsumerV6Test() {

    /**
     * GIVEN no business partner shared
     * WHEN input consumer requests address type count of input stage
     * THEN input consumer sees zero count
     */
    @Test
    fun `count no business partner input exists`(){
        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN legal and site main address inputs
     * WHEN input consumer requests address type count of input stage
     * THEN input consumer sees correct legal and site main address count
     */
    @Test
    fun `count legal and site main address input exists`(){
        //GIVEN
        createBusinessPartnerInputOfType("$testName 1", AddressType.LegalAndSiteMainAddress)
        createBusinessPartnerInputOfType("$testName 2", AddressType.LegalAndSiteMainAddress)
        createBusinessPartnerInputOfType("$testName 3", AddressType.LegalAndSiteMainAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        val expected = StatsAddressTypesResponse(3, 0, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN legal address inputs
     * WHEN input consumer requests address type count of input stage
     * THEN input consumer sees correct legal address count
     */
    @Test
    fun `count legal address input exists`(){
        //GIVEN
        createBusinessPartnerInputOfType("$testName 1", AddressType.LegalAddress)
        createBusinessPartnerInputOfType("$testName 2", AddressType.LegalAddress)
        createBusinessPartnerInputOfType("$testName 3", AddressType.LegalAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        val expected = StatsAddressTypesResponse(0, 3, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN site main address inputs
     * WHEN input consumer requests address type count of input stage
     * THEN input consumer sees correct site main address count
     */
    @Test
    fun `count site main address input exists`(){
        //GIVEN
        createBusinessPartnerInputOfType("$testName 1", AddressType.SiteMainAddress)
        createBusinessPartnerInputOfType("$testName 2", AddressType.SiteMainAddress)
        createBusinessPartnerInputOfType("$testName 3", AddressType.SiteMainAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 3, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN additional address inputs
     * WHEN input consumer requests address type count of input stage
     * THEN input consumer sees correct additional address count
     */
    @Test
    fun `count additional address input exists`(){
        //GIVEN
        createBusinessPartnerInputOfType("$testName 1", AddressType.AdditionalAddress)
        createBusinessPartnerInputOfType("$testName 2", AddressType.AdditionalAddress)
        createBusinessPartnerInputOfType("$testName 3", AddressType.AdditionalAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 0, 3)
        Assertions.assertThat(response).isEqualTo(expected)
    }


    /**
     * GIVEN no business partner shared
     * WHEN input consumer requests address type count of output stage
     * THEN input consumer sees zero count
     */
    @Test
    fun `count no business partner output exists`(){
        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Output)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN legal and site main address Output
     * WHEN input consumer requests address type count of Output stage
     * THEN input consumer sees correct legal and site main address count
     */
    @Test
    fun `count legal and site main address Output exists`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 = testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.refineToLegalEntityOnSite(input1)
        testDataClient.refineToLegalEntityOnSite(input2)
        testDataClient.refineToLegalEntityOnSite(input3)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Output)

        //THEN
        val expected = StatsAddressTypesResponse(3, 0, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN legal address Output
     * WHEN input consumer requests address type count of Output stage
     * THEN input consumer sees correct legal address count
     */
    @Test
    fun `count legal address Output exists`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 = testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.refineToLegalEntity(input1)
        testDataClient.refineToLegalEntity(input2)
        testDataClient.refineToLegalEntity(input3)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Output)

        //THEN
        val expected = StatsAddressTypesResponse(0, 3, 0, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN site main address Output
     * WHEN input consumer requests address type count of Output stage
     * THEN input consumer sees correct site main address count
     */
    @Test
    fun `count site main address Output exists`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 = testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.refineToSite(input1)
        testDataClient.refineToSite(input2)
        testDataClient.refineToSite(input3)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Output)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 3, 0)
        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN additional address Output
     * WHEN input consumer requests address type count of Output stage
     * THEN input consumer sees correct additional address count
     */
    @Test
    fun `count additional address Output exists`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 = testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.refineToAdditionalAddressOfSite(input1)
        testDataClient.refineToAdditionalAddressOfSite(input2)
        testDataClient.refineToAdditionalAddressOfSite(input3)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Output)

        //THEN
        val expected = StatsAddressTypesResponse(0, 0, 0, 3)
        Assertions.assertThat(response).isEqualTo(expected)
    }


    private fun createBusinessPartnerInputOfType(externalId: String, addressType: AddressType) {
        val inputRequest = with(requestFactory.createFullValid(externalId)){
            copy(address = address.copy(addressType = addressType))
        }
        testDataClient.createBusinessPartnerInput(inputRequest)
    }
}