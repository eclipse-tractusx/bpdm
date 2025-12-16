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

import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.junit.jupiter.api.Test

class CountBusinessPartnerSharingStatesV6IT: InputConsumerV6Test() {

    /**
     * GIVEN no business partners
     * WHEN input consumer counts sharing states
     * THEN input consumer sees all zero count
     */
    @Test
    fun `count no sharing states exists`(){
        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(0, 0, 0, 0, 0)

        assertRepo.assertSharingStateStats(response, expected)
    }

    /**
     * GIVEN business partners in initial state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees total initial is correct count
     */
    @Test
    fun `count initial sharing states`(){
        //GIVEN
        testDataClient.createBusinessPartnerInput("$testName 1")
        testDataClient.createBusinessPartnerInput("$testName 2")
        testDataClient.createBusinessPartnerInput("$testName 3")

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(3, 0, 0, 0, 0)

        assertRepo.assertSharingStateStats(response, expected)
    }

    /**
     * GIVEN business partners in ready state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees total ready is correct count
     */
    @Test
    fun `count ready sharing states`(){
        //GIVEN
        val input1 =  testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 =  testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 =  testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToReady(input1.externalId)
        testDataClient.setStateToReady(input2.externalId)
        testDataClient.setStateToReady(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(0, 3, 0, 0, 0)

        assertRepo.assertSharingStateStats(response, expected)
    }

    /**
     * GIVEN business partners in pending state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees total pending is correct count
     */
    @Test
    fun `count pending sharing states`(){
        //GIVEN
        val input1 =  testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 =  testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 =  testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToPending(input1.externalId)
        testDataClient.setStateToPending(input2.externalId)
        testDataClient.setStateToPending(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(0, 0, 3, 0, 0)

        assertRepo.assertSharingStateStats(response, expected)
    }

    /**
     * GIVEN business partners in success state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees total success is correct count
     */
    @Test
    fun `count success sharing states`(){
        //GIVEN
        val input1 =  testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 =  testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 =  testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToSuccess(input1.externalId)
        testDataClient.setStateToSuccess(input2.externalId)
        testDataClient.setStateToSuccess(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(0, 0, 0, 3, 0)

        assertRepo.assertSharingStateStats(response, expected)
    }

    /**
     * GIVEN business partners in error state
     * WHEN input consumer counts sharing states
     * THEN input consumer sees total error is correct count
     */
    @Test
    fun `count error sharing states`(){
        //GIVEN
        val input1 =  testDataClient.createBusinessPartnerInput("$testName 1")
        val input2 =  testDataClient.createBusinessPartnerInput("$testName 2")
        val input3 =  testDataClient.createBusinessPartnerInput("$testName 3")

        testDataClient.setStateToError(input1.externalId)
        testDataClient.setStateToError(input2.externalId)
        testDataClient.setStateToError(input3.externalId)

        //WHEN
        val response = gateClient.stats.countPartnersBySharingState()

        //THEN
        val expected = StatsSharingStatesResponse(0, 0, 0, 0, 3)

        assertRepo.assertSharingStateStats(response, expected)
    }

}