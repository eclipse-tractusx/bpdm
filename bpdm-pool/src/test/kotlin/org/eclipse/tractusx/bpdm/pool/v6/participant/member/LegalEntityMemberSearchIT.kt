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

package org.eclipse.tractusx.bpdm.pool.v6.participant.member

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.participant.ParticipantTest
import org.junit.jupiter.api.Test

class LegalEntityMemberSearchIT: ParticipantTest() {

    /**
     * GIVEN member and non-member legal entity data
     * WHEN participant searches for member legal entity data
     * THEN participant only sees member legal entity
     */
    @Test
    fun `search member legal entities`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        //WHEN
        val searchResponse = poolClient.members.searchLegalEntities(LegalEntitySearchRequest(), PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(member1, member2).map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entity data
     * WHEN participant searches for member legal entity data by BPNLs
     * THEN participant only sees member legal entity with these BPNLs
     */
    @Test
    fun `search member legal entities by BPNL`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(member1.legalEntity.bpnl))
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(member1).map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member and non-member legal entity data
     * WHEN participant searches for member legal entity data by legal name
     * THEN participant only sees member legal entity with such legal name
     */
    @Test
    fun `search member legal entities by legal name`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = member1.legalEntity.legalName)
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(member1).map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member legal entity data with BPNL
     * WHEN participant searches for member legal entity data by that BPNL
     * THEN participant does not find the non-member legal entity
     */
    @Test
    fun `try find non-member legal entities by BPNL`(){
        //GIVEN
        testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(nonMember.legalEntity.bpnl))
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertLegalEntitySearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member legal entity data with legal name
     * WHEN participant searches for member legal entity data by legal name
     * THEN participant does not find the non-member legal entity
     */
    @Test
    fun `try find non-member legal entities by legal name`(){
        //GIVEN
        testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = nonMember.legalEntity.legalName)
        val searchResponse = poolClient.members.searchLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LegalEntityWithLegalAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertLegalEntitySearch(searchResponse, expectedResponse)
    }

}