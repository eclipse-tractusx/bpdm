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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.site

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test


class SiteSearchIT: SharingMemberTest() {

    /**
     * GIVEN sites
     * WHEN sharing member searches for all sites
     * THEN sharing member sees all sites
     */
    @Test
    fun `search sites`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        val siteResponseB = testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        val siteResponseC = testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        val siteResponseD = testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), PaginationRequest())

        //THEN
        val expectedSites = listOf(siteResponseA, siteResponseB, siteResponseC, siteResponseD)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }

        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for sites by BPNS
     * THEN sharing member sees only sites with these BPNS
     */
    @Test
    fun `search sites by BPNS`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        val siteResponseC = testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(siteResponseA.site.bpns, siteResponseC.site.bpns))
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteResponseA, siteResponseC)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }

        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for sites by their legal entity BPNLs
     * THEN sharing member sees only sites from these legal entities
     */
    @Test
    fun `search sites by BPNLs`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        val siteResponseB = testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(legalEntityBpns = listOf(siteResponseA.site.bpnLegalEntity))
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteResponseA, siteResponseB)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }

        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for site by name
     * THEN sharing member sees only sites with this name
     */
    @Test
    fun `search sites by name`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(name = siteResponseA.site.name)
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteResponseA)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }

        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for site page
     * THEN sharing member sees only sites within that page
     */
    @Test
    fun `search sites by page`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        val siteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "$testName A")
        val siteResponseB = testDataClient.createSiteFor(legalEntityResponseA, "$testName B")

        testDataClient.createSiteFor(legalEntityResponseB, "$testName C")
        testDataClient.createSiteFor(legalEntityResponseB, "$testName D")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), paginationRequest)

        //THEN
        val expectedSites = listOf(siteResponseA, siteResponseB)
            .map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }
        val expectedResponse = PageDto(4, 2, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearch(searchResponsePost, expectedResponse)
    }

}