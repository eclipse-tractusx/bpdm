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

package org.eclipse.tractusx.bpdm.gate.v7.stats

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test

class CountAddressTypesV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN no business partners
     * WHEN input consumer requests address type count for input stage
     * THEN input consumer sees zero count
     */
    @Test
    fun `count no business partner input exists`() {
        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsAddressTypesResponse(0, 0, 0, 0))
    }

    /**
     * GIVEN legal and site main address inputs
     * WHEN input consumer requests address type count for input stage
     * THEN input consumer sees correct legal and site main address count
     */
    @Test
    fun `count legal and site main address inputs`() {
        //GIVEN
        createInputOfType("$testName 1", AddressType.LegalAndSiteMainAddress)
        createInputOfType("$testName 2", AddressType.LegalAndSiteMainAddress)
        createInputOfType("$testName 3", AddressType.LegalAndSiteMainAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsAddressTypesResponse(3, 0, 0, 0))
    }

    /**
     * GIVEN legal address inputs
     * WHEN input consumer requests address type count for input stage
     * THEN input consumer sees correct legal address count
     */
    @Test
    fun `count legal address inputs`() {
        //GIVEN
        createInputOfType("$testName 1", AddressType.LegalAddress)
        createInputOfType("$testName 2", AddressType.LegalAddress)
        createInputOfType("$testName 3", AddressType.LegalAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsAddressTypesResponse(0, 3, 0, 0))
    }

    /**
     * GIVEN site main address inputs
     * WHEN input consumer requests address type count for input stage
     * THEN input consumer sees correct site main address count
     */
    @Test
    fun `count site main address inputs`() {
        //GIVEN
        createInputOfType("$testName 1", AddressType.SiteMainAddress)
        createInputOfType("$testName 2", AddressType.SiteMainAddress)
        createInputOfType("$testName 3", AddressType.SiteMainAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsAddressTypesResponse(0, 0, 3, 0))
    }

    /**
     * GIVEN additional address inputs
     * WHEN input consumer requests address type count for input stage
     * THEN input consumer sees correct additional address count
     */
    @Test
    fun `count additional address inputs`() {
        //GIVEN
        createInputOfType("$testName 1", AddressType.AdditionalAddress)
        createInputOfType("$testName 2", AddressType.AdditionalAddress)
        createInputOfType("$testName 3", AddressType.AdditionalAddress)

        //WHEN
        val response = gateClient.stats.countAddressTypes(StageType.Input)

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsAddressTypesResponse(0, 0, 0, 3))
    }

    private fun createInputOfType(seed: String, addressType: AddressType) {
        val request = businessPartnerInputRequestFactory.fromSeed(seed)
            .copy(address = businessPartnerInputRequestFactory.fromSeed(seed).address.copy(addressType = addressType))
        testDataClient.upsertBusinessPartnerInput(request)
    }
}
