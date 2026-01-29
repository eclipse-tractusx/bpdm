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

package org.eclipse.tractusx.bpdm.pool.v6.site

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolV6Test
import org.junit.jupiter.api.Test

class LegalEntitySiteGetV6IT: UnscheduledPoolV6Test() {

    /**
     * GIVEN sites of legal entity
     * WHEN sharing member fetches all sites of legal entity
     * THEN sharing member sees all sites of that legal entity
     */
    @Test
    fun `fetch all sites of a legal entity`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        val siteResponseB = testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val fetchResponse = poolClient.legalEntities.getSites(legalEntityResponseA.legalEntity.bpnl, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteResponseA, siteResponseB)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it).site }
        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteVerbose(fetchResponse, expectedResponse)
    }

    /**
     * GIVEN sites of legal entity
     * WHEN sharing member fetches page of sites for legal entity
     * THEN sharing member sees only sites of the page of that legal entity
     */
    @Test
    fun `fetch site page of a legal entity`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val fetchResponse = poolClient.legalEntities.getSites(legalEntityResponseA.legalEntity.bpnl, PaginationRequest(0, 1))

        //THEN
        val expectedSites = listOf(siteResponseA)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it).site }
        val expectedResponse = PageDto(2, 2, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteVerbose(fetchResponse, expectedResponse)
    }
}