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
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class LegalEntitySearchV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for all legal entities
     * THEN sharing member sees all legal entities
     */
    @Test
    fun `search all legal entities`(){
        //GIVEN
        val legalEntity1 = testDataClient.createLegalEntity("$testName 1")
        val legalEntity2 = testDataClient.createLegalEntity("$testName 2")

        //WHEN
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(legalEntity1, legalEntity2))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entities by BPNLs
     * THEN sharing member sees only legal entities with these BPNLs
     */
    @Test
    fun `search legal entities by BPNLs`(){
        //GIVEN
        val legalEntity1 = testDataClient.createLegalEntity("$testName 1")
        val legalEntity2 = testDataClient.createLegalEntity("$testName 2")
        testDataClient.createLegalEntity("$testName 3")
        testDataClient.createLegalEntity("$testName 4")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(bpnLs = listOf(legalEntity1.header.bpnl, legalEntity2.header.bpnl))
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(legalEntity1, legalEntity2))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entity by legal name
     * THEN sharing member sees only legal entities with this legal name
     */
    @Test
    fun `search legal entities by legal name`(){
        //GIVEN
        val legalEntity1 = testDataClient.createLegalEntity("$testName 1")
        testDataClient.createLegalEntity("$testName 2")
        testDataClient.createLegalEntity("$testName 3")
        testDataClient.createLegalEntity("$testName 4")

        //WHEN
        val searchRequest = LegalEntitySearchRequest(legalName = legalEntity1.header.legalName)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(legalEntity1))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for legal entity page
     * THEN sharing member sees only legal entities within that page
     */
    @Test
    fun `search legal entities by first page`(){
        //GIVEN
        val legalEntity1 = testDataClient.createLegalEntity("$testName 1")
        val legalEntity2 = testDataClient.createLegalEntity("$testName 2")
        testDataClient.createLegalEntity("$testName 3")
        testDataClient.createLegalEntity("$testName 4")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), paginationRequest)

        //THEN
        val expectedResponse = PageDto(4, 2, 0, 2, listOf(legalEntity1, legalEntity2))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities
     * WHEN sharing member searches for the second legal entity page
     * THEN sharing member sees only legal entities on that page
     */
    @Test
    fun `search legal entities by second page`(){
        //GIVEN
        testDataClient.createLegalEntity("$testName 1")
        testDataClient.createLegalEntity("$testName 2")
        val legalEntity3 = testDataClient.createLegalEntity("$testName 3")
        val legalEntity4 = testDataClient.createLegalEntity("$testName 4")

        //WHEN
        val paginationRequest = PaginationRequest(1, 2)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), paginationRequest)

        //THEN
        val expectedResponse = PageDto(4, 2, 1, 2, listOf(legalEntity3, legalEntity4))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN legal entities where total count is not divisible by page size
     * WHEN sharing member searches for the last legal entity page
     * THEN sharing member sees only the remaining legal entities on that page
     */
    @Test
    fun `search legal entities by partial last page`(){
        //GIVEN
        testDataClient.createLegalEntity("$testName 1")
        testDataClient.createLegalEntity("$testName 2")
        val legalEntity3 = testDataClient.createLegalEntity("$testName 3")

        //WHEN
        val paginationRequest = PaginationRequest(1, 2)
        val searchResponseGet = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.legalEntities.postLegalEntitySearch(LegalEntitySearchRequest(), paginationRequest)

        //THEN
        val expectedResponse = PageDto(3, 2, 1, 1, listOf(legalEntity3))

        assertRepository.assertLegalEntitySearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertLegalEntitySearchResponse(searchResponsePost, expectedResponse)
    }
}