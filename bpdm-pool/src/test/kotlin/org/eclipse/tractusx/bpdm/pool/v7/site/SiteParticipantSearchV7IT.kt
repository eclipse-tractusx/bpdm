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
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class SiteParticipantSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN participant and non-participant legal entity data with sites
     * WHEN participant searches for participant sites
     * THEN participant only sees sites of participant legal entities
     */
    @Test
    fun `search participant sites`() {
        //GIVEN
        val participantLE1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        val participantLE2 = testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipantLE = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite1 = testDataClient.createSite(participantLE1, "$testName Site 1")
        val participantSite2 = testDataClient.createSite(participantLE2, "$testName Site 2")
        testDataClient.createSite(nonParticipantLE, "$testName Site 3")

        //WHEN
        val searchResponse = poolClient.members.postSiteSearch(SiteSearchRequest(), PaginationRequest())

        //THEN
        val expectedSites = listOf(participantSite1, participantSite2).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entity data with sites
     * WHEN participant searches for participant sites by BPNS
     * THEN participant only sees participant site with that BPNS
     */
    @Test
    fun `search participant sites by BPNS`() {
        //GIVEN
        val participantLE1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        val participantLE2 = testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipantLE = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite1 = testDataClient.createSite(participantLE1, "$testName Site 1")
        testDataClient.createSite(participantLE2, "$testName Site 2")
        testDataClient.createSite(nonParticipantLE, "$testName Site 3")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(participantSite1.site.bpns))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(participantSite1).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant and non-participant legal entity data with sites
     * WHEN participant searches for participant sites by parent legal entity BPNL
     * THEN participant only sees sites of that participant legal entity
     */
    @Test
    fun `search participant sites by parent BPNL`() {
        //GIVEN
        val participantLE1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        val participantLE2 = testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipantLE = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite1 = testDataClient.createSite(participantLE1, "$testName Site 1")
        testDataClient.createSite(participantLE2, "$testName Site 2")
        testDataClient.createSite(nonParticipantLE, "$testName Site 3")

        //WHEN
        val searchRequest = SiteSearchRequest(legalEntityBpns = listOf(participantLE1.header.bpnl))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(participantSite1).map { resultFactory.buildSiteSearchResponse(it) }
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedSites)

        assertRepository.assertSiteSearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity data with site
     * WHEN participant searches for participant sites by that site BPNS
     * THEN participant does not find the non-participant site
     */
    @Test
    fun `try find non-participant site by BPNS`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName Participant")
        val nonParticipantLE = testDataClient.createLegalEntity("$testName Non-Participant")
        val nonParticipantSite = testDataClient.createSite(nonParticipantLE, "$testName Site")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(nonParticipantSite.site.bpns))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<SiteWithMainAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertSiteSearchResponse(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity data with site
     * WHEN participant searches for participant sites by the non-participant parent BPNL
     * THEN participant does not find the non-participant site
     */
    @Test
    fun `try find non-participant site by parent BPNL`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName Participant")
        val nonParticipantLE = testDataClient.createLegalEntity("$testName Non-Participant")
        testDataClient.createSite(nonParticipantLE, "$testName Site")

        //WHEN
        val searchRequest = SiteSearchRequest(legalEntityBpns = listOf(nonParticipantLE.header.bpnl))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<SiteWithMainAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertSiteSearchResponse(searchResponse, expectedResponse)
    }
}
