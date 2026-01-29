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

package org.eclipse.tractusx.bpdm.pool.v6.legalentity

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolV6Test
import org.junit.jupiter.api.Test

class LegalEntityGetSearchV6IT: UnscheduledPoolV6Test()
{
    /**
     * GIVEN legal entities
     * WHEN sharing member searches for all legal entities
     * THEN sharing member sees all legal entities
     */
    @Test
    fun `search legal entities`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")
        val legalEntityResponseC =  testDataClient.createLegalEntity("$testName C")
        val legalEntityResponseD =  testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(legalEntityResponseA, legalEntityResponseB, legalEntityResponseC, legalEntityResponseD)
            .map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }

        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entities by BPNLs
     * THEN sharing member sees only legal entities with these BPNLs
     */
    @Test
    fun `search legal entities by BPNLs`(){

        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(legalEntityResponseA.legalEntity.bpnl, legalEntityResponseB.legalEntity.bpnl))
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(searchRequest, PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(legalEntityResponseA, legalEntityResponseB)
            .map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }

        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entity by legal name
     * THEN sharing member sees only legal entities with this legal name
     */
    @Test
    fun `search legal entities by legal name`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = legalEntityResponseA.legalEntity.legalName)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedLegalEntities = listOf(legalEntityResponseA).map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(expectedLegalEntities.size.toLong(), 1, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entity page
     * THEN sharing member sees only legal entities within that page
     */
    @Test
    fun `search legal entities by page`(){
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB = testDataClient.createLegalEntity("$testName B")
        testDataClient.createLegalEntity("$testName C")
        testDataClient.createLegalEntity("$testName D")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), paginationRequest)

        //THEN
        val expectedLegalEntities = listOf(legalEntityResponseA, legalEntityResponseB).map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(4, 2, 0, expectedLegalEntities.size, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearch(searchResponsePost, expectedResponse)
    }
}