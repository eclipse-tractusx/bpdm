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

package org.eclipse.tractusx.bpdm.pool.v7.site

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class SiteSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN sites
     * WHEN sharing member searches for all sites
     * THEN sharing member sees all sites
     */
    @Test
    fun `search all sites`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        val siteA = testDataClient.createLegalAddressSite(legalEntityA, "$testName A")
        val siteB = testDataClient.createSite(legalEntityA, "$testName B")
        val siteC = testDataClient.createSite(legalEntityB, "$testName C")
        val siteD = testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), PaginationRequest())

        //THEN
        val expectedSites = listOf(siteA, siteB, siteC, siteD).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for sites by BPNS
     * THEN sharing member sees only sites with these BPNS
     */
    @Test
    fun `search sites by BPNS`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        val siteA = testDataClient.createLegalAddressSite(legalEntityA, "$testName A")
        testDataClient.createSite(legalEntityA, "$testName B")
        val siteC = testDataClient.createSite(legalEntityB, "$testName C")
        testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(siteA.site.bpns, siteC.site.bpns))
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteA, siteC).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for sites by their legal entity BPNLs
     * THEN sharing member sees only sites from these legal entities
     */
    @Test
    fun `search sites by BPNLs`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        val siteA = testDataClient.createLegalAddressSite(legalEntityA, "$testName A")
        val siteB = testDataClient.createSite(legalEntityA, "$testName B")
        testDataClient.createSite(legalEntityB, "$testName C")
        testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(legalEntityBpns = listOf(siteA.site.bpnLegalEntity))
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteA, siteB).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for site by name
     * THEN sharing member sees only sites with this name
     */
    @Test
    fun `search sites by name`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        val siteA = testDataClient.createLegalAddressSite(legalEntityA, "$testName A")
        testDataClient.createSite(legalEntityA, "$testName B")
        testDataClient.createSite(legalEntityB, "$testName C")
        testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val searchRequest = SiteSearchRequest(name = siteA.site.name)
        val searchResponseGet = poolClient.sites.getSites(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.sites.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(siteA).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for the first site page
     * THEN sharing member sees only sites within that page
     */
    @Test
    fun `search sites by first page`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        val siteA = testDataClient.createSite(legalEntityA, "$testName A")
        val siteB = testDataClient.createSite(legalEntityA, "$testName B")
        testDataClient.createSite(legalEntityB, "$testName C")
        testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), paginationRequest)

        //THEN
        val expectedSites = listOf(siteA, siteB).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = PageDto(4, 2, 0, 2, expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites
     * WHEN sharing member searches for the second site page
     * THEN sharing member sees only sites on that page
     */
    @Test
    fun `search sites by second page`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        testDataClient.createSite(legalEntityA, "$testName A")
        testDataClient.createSite(legalEntityA, "$testName B")
        val siteC = testDataClient.createSite(legalEntityB, "$testName C")
        val siteD = testDataClient.createSite(legalEntityB, "$testName D")

        //WHEN
        val paginationRequest = PaginationRequest(1, 2)
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), paginationRequest)

        //THEN
        val expectedSites = listOf(siteC, siteD).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = PageDto(4, 2, 1, 2, expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN sites where total count is not divisible by page size
     * WHEN sharing member searches for the last site page
     * THEN sharing member sees only the remaining sites on that page
     */
    @Test
    fun `search sites by partial last page`() {
        //GIVEN
        val legalEntityA = testDataClient.createParticipantLegalEntity("$testName A")
        val legalEntityB = testDataClient.createParticipantLegalEntity("$testName B")

        testDataClient.createSite(legalEntityA, "$testName A")
        testDataClient.createSite(legalEntityA, "$testName B")
        val siteC = testDataClient.createSite(legalEntityB, "$testName C")

        //WHEN
        val paginationRequest = PaginationRequest(1, 2)
        val searchResponseGet = poolClient.sites.getSites(SiteSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.sites.postSiteSearch(SiteSearchRequest(), paginationRequest)

        //THEN
        val expectedSites = listOf(siteC).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = PageDto(3, 2, 1, 1, expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponseGet, expectedResponse)
        assertRepository.assertSiteSearchResponse(searchResponsePost, expectedResponse)
    }
}
