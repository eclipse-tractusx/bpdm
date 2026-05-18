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

import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.junit.jupiter.api.Test

class CountBusinessPartnerSharingStatesV7IT : UnscheduledGateTestBaseV7() {

    /**
     * GIVEN no business partners
     * WHEN input consumer counts sharing states
     * THEN input consumer sees all zero counts
     */
    @Test
    fun `count no sharing states exist`() {
        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        assertRepo.assertSharingStateStats(response, StatsSharingStatesResponse(0, 0, 0, 0, 0))
    }

    /**
     * GIVEN business partners in initial state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees correct initial count
     */
    @Test
    fun `count initial sharing states`() {
        //GIVEN
        testDataClient.businessPartner.upsertInput("$testName 1")
        testDataClient.businessPartner.upsertInput("$testName 2")
        testDataClient.businessPartner.upsertInput("$testName 3")

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        assertRepo.assertSharingStateStats(response, StatsSharingStatesResponse(3, 0, 0, 0, 0))
    }

    /**
     * GIVEN business partners set to ready state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees correct ready count
     */
    @Test
    fun `count ready sharing states`() {
        //GIVEN
        val input1 = testDataClient.businessPartner.upsertInput("$testName 1")
        val input2 = testDataClient.businessPartner.upsertInput("$testName 2")
        val input3 = testDataClient.businessPartner.upsertInput("$testName 3")

        testDataClient.businessPartner.setStateToReady(input1.externalId)
        testDataClient.businessPartner.setStateToReady(input2.externalId)
        testDataClient.businessPartner.setStateToReady(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        assertRepo.assertSharingStateStats(response, StatsSharingStatesResponse(0, 3, 0, 0, 0))
    }
}
