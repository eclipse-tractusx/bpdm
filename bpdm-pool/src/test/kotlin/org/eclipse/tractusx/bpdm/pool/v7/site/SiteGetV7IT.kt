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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class SiteGetV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN operator created site
     * WHEN operator searches for created site by BPNs
     * THEN operator finds created site
     */
    @Test
    fun `create valid site and find it`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val response = poolClient.sites.postSiteSearch(SiteSearchRequest(siteBpns = listOf(siteResponse.site.bpns)), PaginationRequest())

        //THEN
        val expectedSite = resultFactory.buildSiteSearchResponse(siteResponse)
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(expectedSite))

        assertRepository.assertSiteSearchResponse(response, expectedResponse)
    }

    /**
     * GIVEN site with legal entity having BPNL
     * WHEN sharing member requests sites by parent legal entity BPNL
     * THEN sharing member sees site
     */
    @Test
    fun `find site by parent legal entity BPNL`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val response = poolClient.sites.postSiteSearch(SiteSearchRequest(legalEntityBpns = listOf(legalEntityResponse.header.bpnl)), PaginationRequest())

        //THEN
        val expectedSite = resultFactory.buildSiteSearchResponse(siteResponse)
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(expectedSite))

        assertRepository.assertSiteSearchResponse(response, expectedResponse)
    }

    /**
     * WHEN sharing member requests site by unknown BPNS
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try get site by unknown BPNS`() {
        //WHEN
        val unknownGet = { poolClient.sites.getSite("UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }

    /**
     * GIVEN site with BPNS that has been updated before
     * WHEN sharing member requests site with BPNS
     * THEN sharing member sees site with that values
     */
    @Test
    fun `get site after update`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val updatedSiteResponse = testDataClient.updateSite(siteResponse, "Updated $testName")

        //WHEN
        val fetchedSite = poolClient.sites.getSite(updatedSiteResponse.site.bpns)

        //THEN
        val expectedSite = resultFactory.buildSiteSearchResponse(updatedSiteResponse)

        assertRepository.assertSiteGetIsEqual(fetchedSite, expectedSite)
    }
}