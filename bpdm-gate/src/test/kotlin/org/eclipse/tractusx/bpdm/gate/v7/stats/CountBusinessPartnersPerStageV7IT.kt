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
import org.eclipse.tractusx.bpdm.gate.api.v7.model.stats.StatsStagesV7Response
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test

class CountBusinessPartnersPerStageV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN no business partners
     * WHEN input consumer requests count per stage
     * THEN input consumer sees zero total count
     */
    @Test
    fun `count no business partners exist`() {
        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsStagesV7Response(0, 0))
    }

    /**
     * GIVEN golden and alternative-process business partners and outputs
     * WHEN input consumer requests count per stage
     * THEN only golden-process records are counted
     */
    @Test
    fun `alternative process partners and outputs are not counted`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput("$testName golden")
        testDataClient.businessPartner.createOutput("$testName golden output")
        val alternative = testDataClient.businessPartner.upsertInput("$testName alternative", "provider")
        testDataClient.businessPartner.refineAlternatively(alternative, "provider")

        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsStagesV7Response(2, 1))
    }

    @Test
    fun `count previous golden output until alternative process overwrites it`() {
        //GIVEN
        val output = testDataClient.businessPartner.createOutput(testName)

        //WHEN
        val input = testDataClient.businessPartner.upsertInput(output.externalId, "provider")

        //THEN
        Assertions.assertThat(gateClient.stats.countPartnersPerStage()).isEqualTo(StatsStagesV7Response(0, 1))

        //WHEN
        testDataClient.businessPartner.refineAlternatively(input, "provider")

        //THEN
        Assertions.assertThat(gateClient.stats.countPartnersPerStage()).isEqualTo(StatsStagesV7Response(0, 0))
    }

    /**
     * GIVEN business partner inputs
     * WHEN input consumer requests count per stage
     * THEN input consumer sees correct input count
     */
    @Test
    fun `count business partner inputs`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput("$testName 1")
        testDataClient.businessPartner.upsertInput("$testName 2")
        testDataClient.businessPartner.upsertInput("$testName 3")

        //WHEN
        val response = gateClient.stats.countPartnersPerStage()

        //THEN
        Assertions.assertThat(response).isEqualTo(StatsStagesV7Response(3, 0))
    }
}
