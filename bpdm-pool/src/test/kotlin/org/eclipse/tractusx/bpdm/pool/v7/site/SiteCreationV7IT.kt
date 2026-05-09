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

import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withAlternativeAdminArea
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withMainAddressIdentifiers
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withPhysicalAdminArea
import org.junit.jupiter.api.Test

class SiteCreationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN participant legal entity
     * WHEN operator creates a new valid site for legal entity
     * THEN created site returned
     */
    @Test
    fun `create valid site`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("Site $testName", legalEntityResponse)
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedSite = resultFactory.buildSiteSiteCreate(siteRequest)
        val expectedResponse = SitePartnerCreateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator creates a valid site for legal entity that has the legal address as site main address
     * THEN created site returned
     */
    @Test
    fun `create valid legal address site`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildLegalAddressSiteCreateRequest("Site $testName", legalEntityResponse)
        val response = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest))

        //THEN
        val expectedSite = resultFactory.buildLegalSiteCreate(siteRequest, legalEntityResponse)
        val expectedResponse = SitePartnerCreateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address site
     * WHEN operator tries to create a new legal address site
     * THEN operator sees MainAddressDuplicateIdentifier error
     */
    @Test
    fun `try create duplicate legal address site`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        testDataClient.createLegalAddressSite(legalEntityResponse, testName)

        //WHEN
        val siteRequest = requestFactory.buildLegalAddressSiteCreateRequest("New Legal Address Site $testName", legalEntityResponse)
        val response = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", siteRequest.name)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity and site with main address identifier X
     * WHEN operator tries to create a new site for legal entity with main address identifier X
     * THEN operator sees MainAddressDuplicateIdentifier error
     */
    @Test
    fun `try create site with duplicate address identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val givenSiteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val identifierX = givenSiteResponse.mainAddress.identifiers.first()

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", legalEntityResponse)
            .withMainAddressIdentifiers(AddressIdentifierDto(identifierX.value, identifierX.typeVerbose.technicalKey))
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create two new sites for legal entity with the same main address identifier
     * THEN operator sees MainAddressDuplicateIdentifier error
     */
    @Test
    fun `try create site having duplicate address identifiers`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest1 = requestFactory.buildSiteCreateRequest("Site 1 $testName", legalEntityResponse)
        val sharedIdentifier = siteRequest1.site.mainAddress.identifiers.first()
        val siteRequest2 = requestFactory.buildSiteCreateRequest("Site 2 $testName", legalEntityResponse)
            .withMainAddressIdentifiers(sharedIdentifier)
        val response = poolClient.sites.createSite(listOf(siteRequest1, siteRequest2))

        //THEN
        val expectedErrors = listOf(siteRequest1, siteRequest2).map { ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown identifier type
     * THEN operator sees MainAddressIdentifierNotFound error
     */
    @Test
    fun `try create site with unknown address identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", legalEntityResponse)
            .withMainAddressIdentifiers(AddressIdentifierDto(value = "Any Value", type = "UNKNOWN"))
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressIdentifierNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new site for unknown legal entity
     * THEN operator sees LegalEntityNotFound error
     */
    @Test
    fun `try create site with unknown legal entity`() {
        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", "UNKNOWN")
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.LegalEntityNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown physical region
     * THEN operator sees MainAddressRegionNotFound error
     */
    @Test
    fun `try create site with unknown physical region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", legalEntityResponse)
            .withPhysicalAdminArea("UNKNOWN")
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressRegionNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown alternative region
     * THEN operator sees MainAddressRegionNotFound error
     */
    @Test
    fun `try create site with unknown alternative region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", legalEntityResponse)
            .withAlternativeAdminArea("UNKNOWN")
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressRegionNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with too many identifiers
     * THEN operator sees MainAddressIdentifiersTooMany error
     */
    @Test
    fun `try create site with too many identifiers`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val siteRequest = requestFactory.buildSiteCreateRequest("New Site $testName", legalEntityResponse)
            .withMainAddressIdentifiers((1..101).map { requestFactory.buildAddressIdentifier(testName, it) })
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressIdentifiersTooMany, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertSiteCreateResponseWrapperIsEqual(response, expectedResponse)
    }
}
