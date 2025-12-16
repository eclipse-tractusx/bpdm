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
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsStagesResponse
import org.junit.jupiter.api.Test

class CountBusinessPartnersPerStageV6IT: InputConsumerV6Test() {

    /**
     * GIVEN no business partners shared
     * WHEN input consumer requests count of business partners per stage
     * THEN input consumer sees zero total count
     */
    @Test
    fun `count no business partners exists`(){
        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        val expected = StatsStagesResponse(0, 0)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN business partner inputs
     * WHEN input consumer requests count of business partners per stage
     * THEN input consumer sees correct total input count
     */
    @Test
    fun `count business partners inputs`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")
        testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        val expected = StatsStagesResponse(3, 0)

        Assertions.assertThat(response).isEqualTo(expected)
    }

    /**
     * GIVEN business partner outputs
     * WHEN input consumer requests count of business partners per stage
     * THEN input consumer sees correct total output count
     */
    @Test
    fun `count business partners outputs`(){
        //GIVEN
        val input1 = testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 = testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 = testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToSuccess(input1.externalId)
        testDataClient.setStateToSuccess(input2.externalId)
        testDataClient.setStateToSuccess(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        val expected = StatsStagesResponse(3, 3)

        Assertions.assertThat(response).isEqualTo(expected)
    }
}