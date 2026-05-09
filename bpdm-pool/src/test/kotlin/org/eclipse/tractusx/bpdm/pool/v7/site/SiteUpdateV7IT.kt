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

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withAlternativeAdminArea
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withMainAddressIdentifiers
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withPhysicalAdminArea
import org.junit.jupiter.api.Test

class SiteUpdateV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN site
     * WHEN operator updates site information
     * THEN operator sees updated site information
     */
    @Test
    fun `update valid site`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val siteUpdateRequest = requestFactory.createSiteUpdateRequest("Site Update $testName", siteResponse)
        val response = poolClient.sites.updateSite(listOf(siteUpdateRequest))

        //THEN
        val expectedSite = resultFactory.buildSiteUpdate(siteUpdateRequest, siteResponse)
        val expectedResponse = SitePartnerUpdateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN operator updated site
     * WHEN operator searches for updated site
     * THEN operator finds updated site information
     */
    @Test
    fun `update valid site and find it`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val updatedSiteResponse = testDataClient.updateSite(siteResponse, "Site Update $testName")

        //WHEN
        val response = poolClient.sites.postSiteSearch(SiteSearchRequest(siteBpns = listOf(updatedSiteResponse.site.bpns)), PaginationRequest())

        //THEN
        val expectedSite = resultFactory.buildSiteSearchResponse(updatedSiteResponse)
        val expectedResponse = resultFactory.buildSinglePageResponse(listOf(expectedSite))

        assertRepository.assertSiteSearchResponse(response, expectedResponse)
    }

    /**
     * WHEN operator tries to update site for an unknown BPNS
     * THEN operator sees site not found error
     */
    @Test
    fun `try update site with unknown bpn`() {
        //WHEN
        val siteUpdateRequest = requestFactory.createSiteUpdateRequest(testName, "UNKNOWN")
        val response = poolClient.sites.updateSite(listOf(siteUpdateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.SiteNotFound, "IGNORED", "UNKNOWN")
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN site A with identifier X and site B
     * WHEN operator tries to update site B with identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try update site with duplicate address identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponseA = testDataClient.createSite(legalEntityResponse, "Site A $testName")
        val identifierX = siteResponseA.mainAddress.identifiers.first()
        val siteResponseB = testDataClient.createSite(legalEntityResponse, "Site B $testName")

        //WHEN
        val updateRequest = requestFactory.createSiteUpdateRequest("Site B Update $testName", siteResponseB)
            .withMainAddressIdentifiers(AddressIdentifierDto(identifierX.value, identifierX.typeVerbose.technicalKey))
        val response = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressDuplicateIdentifier, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown identifier type
     * THEN operator sees identifier not found error
     */
    @Test
    fun `try update site with unknown address identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val updateRequest = requestFactory.createSiteUpdateRequest("New Site $testName", siteCreateResponse)
            .withMainAddressIdentifiers(AddressIdentifierDto(value = "Any Value", type = "UNKNOWN"))
        val response = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressIdentifierNotFound, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update site with unknown physical region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val updateRequest = requestFactory.createSiteUpdateRequest("New Site $testName", siteCreateResponse)
            .withPhysicalAdminArea("UNKNOWN")
        val response = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressRegionNotFound, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update site with unknown alternative region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val updateRequest = requestFactory.createSiteUpdateRequest("New Site $testName", siteCreateResponse)
            .withAlternativeAdminArea("UNKNOWN")
        val response = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressRegionNotFound, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with too many identifiers
     * THEN operator sees too many identifiers error
     */
    @Test
    fun `try update site with too many identifiers`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val updateRequest = requestFactory.createSiteUpdateRequest("New Site $testName", siteCreateResponse)
            .withMainAddressIdentifiers((1..101).map { requestFactory.buildAddressIdentifier(testName, it) })
        val response = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressIdentifiersTooMany, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteUpdateResponseWrapperIsEqual(response, expectedResponse)
    }
}
