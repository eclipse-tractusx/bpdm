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

package org.eclipse.tractusx.bpdm.pool.v7.legalentity

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.junit.jupiter.api.Test

class LegalEntityParticipantSearchV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN participant and non-participant legal entity data
     * WHEN participant searches for participant legal entity data
     * THEN participant only sees participant legal entities
     */
    @Test
    fun `search participant legal entities`(){
        //GIVEN
        val participant1 = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 1").withParticipantData(true))
        val participant2 = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 2").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Non-Participant").withParticipantData(false))

        //WHEN
        val searchResponse = poolClient.members.searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(participant1, participant2))

        assertRepository.assertLegalEntitySearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entity data
     * WHEN participant searches for participant legal entity data by BPNL
     * THEN participant only sees participant legal entity with that BPNL
     */
    @Test
    fun `search participant legal entities by BPNL`(){
        //GIVEN
        val participant1 = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 1").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 2").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Non-Participant").withParticipantData(false))

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(participant1.header.bpnl))
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(participant1))

        assertRepository.assertLegalEntitySearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entity data
     * WHEN participant searches for participant legal entity data by legal name
     * THEN participant only sees participant legal entity with that legal name
     */
    @Test
    fun `search participant legal entities by legal name`(){
        //GIVEN
        val participant1 = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 1").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 2").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Non-Participant").withParticipantData(false))

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = participant1.header.legalName)
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(participant1))

        assertRepository.assertLegalEntitySearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity data with BPNL
     * WHEN participant searches for participant legal entity data by that BPNL
     * THEN participant does not find the non-participant legal entity
     */
    @Test
    fun `try find non-participant legal entity by BPNL`(){
        //GIVEN
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 1").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 2").withParticipantData(true))
        val nonParticipant = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Non-Participant").withParticipantData(false))

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(nonParticipant.header.bpnl))
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertLegalEntitySearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity data with legal name
     * WHEN participant searches for participant legal entity data by that legal name
     * THEN participant does not find the non-participant legal entity
     */
    @Test
    fun `try find non-participant legal entity by legal name`(){
        //GIVEN
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 1").withParticipantData(true))
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Participant 2").withParticipantData(true))
        val nonParticipant = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName Non-Participant").withParticipantData(false))

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = nonParticipant.header.legalName)
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertLegalEntitySearchResponse(searchResponse, expectedResponse)
    }

}
