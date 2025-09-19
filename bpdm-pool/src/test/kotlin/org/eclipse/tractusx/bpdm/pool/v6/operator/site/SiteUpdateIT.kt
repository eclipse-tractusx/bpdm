/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v6.operator.site

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.pool.v6.util.PoolOperatorClientV6
import org.eclipse.tractusx.bpdm.pool.v6.util.TestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SiteUpdateIT @Autowired constructor(
    private val poolClient: PoolOperatorClientV6,
    private val testDataV6Factory: TestDataV6Factory,
    private val assertRepo: AssertRepositoryV6,
    private val testDataClient: TestDataClientV6
): OperatorTest() {

    /**
     * GIVEN site
     * WHEN operator updates site information
     * THEN operator sees updated site information
     */
    @Test
    fun `update valid site`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val siteUpdateRequest = testDataV6Factory.request.createSiteUpdateRequest("Site Update $testName", siteResponse)
        val response = poolClient.sites.updateSite(listOf(siteUpdateRequest))

        //THEN
        val expectedSite = testDataV6Factory.result.buildExpectedSiteUpdateResponse(siteUpdateRequest, siteResponse, legalEntityResponse)
        val expectedResponse = SitePartnerUpdateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepo.assertSiteUpdate(response, expectedResponse)
    }

    /**
     * GIVEN operator updated site
     * WHEN operator searches for updated site
     * THEN operator finds updated site information
     */
    @Test
    fun `update valid site and find it`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        val siteUpdateRequest = testDataV6Factory.request.createSiteUpdateRequest("Site Update $testName", siteResponse.site.bpns)
        val updatedSiteResponse = poolClient.sites.updateSite(listOf(siteUpdateRequest)).entities.single()

        //WHEN
        val response = poolClient.sites.postSiteSearch(SiteSearchRequest(siteBpns = listOf(updatedSiteResponse.site.bpns)), PaginationRequest())

        //THEN
        val expectedSite = SiteWithMainAddressVerboseDto(updatedSiteResponse.site, updatedSiteResponse.mainAddress)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedSite))

        assertRepo.assertSiteSearch(response, expectedResponse)
    }

    /**
     * WHEN operator tries to update site for an unknown BPNS
     * THEN operator sees site not found error
     */
    @Test
    fun `try update site with unknown bpn`(){
        //WHEN
        val siteUpdateRequest = testDataV6Factory.request.createSiteUpdateRequest(testName, "UNKNOWN")
        val response = poolClient.sites.updateSite(listOf(siteUpdateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.SiteNotFound, "IGNORED", "UNKNOWN")
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(response, expectedResponse)
    }


    /**
     * GIVEN site A with identifier X and site B
     * WHEN operator tries to update site B with identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try update site with duplicate address identifier`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        val siteResponseA = testDataClient.createSiteFor(legalEntityResponse, "Site A $testName")
        val identifierX = siteResponseA.mainAddress.identifiers.first()

        val siteResponseB = testDataClient.createSiteFor(legalEntityResponse, "Site B $testName")

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createSiteUpdateRequest("Site B Update $testName", siteResponseB)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = listOf(AddressIdentifierDto(identifierX.value, identifierX.type)))))
        }
        val siteResponse = poolClient.sites.updateSite(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressDuplicateIdentifier, "IGNORED", updateRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN site A and site B
     * WHEN operator tries to update the sites with the same main address identifier
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try update sites having duplicate address identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val siteRequest1 = testDataV6Factory.request.buildSiteCreateRequest("Site 1 $testName", legalEntityResponse)
        val sameIdentifier = siteRequest1.site.mainAddress.identifiers.first()

        val siteRequest2 = with(testDataV6Factory.request.buildSiteCreateRequest("Site 2 $testName", legalEntityResponse)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = listOf(sameIdentifier))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest1, siteRequest2))

        //THEN
        val expectedErrors = listOf(siteRequest1, siteRequest2).map { ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown identifier type
     * THEN operator sees identifier not found error
     */
    @Test
    fun `try update site with unknown address identifier`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteUpdateRequest("New Site $testName", siteCreateResponse)){
            val unknownIdentifier = site.mainAddress.identifiers.first().copy(type = "UNKNOWN")
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = site.mainAddress.identifiers.drop(1).plus(unknownIdentifier))))
        }
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressIdentifierNotFound, "IGNORED", siteRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update site with unknown physical region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteUpdateRequest("New Site $testName", siteCreateResponse)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN"))))
        }
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressRegionNotFound, "IGNORED", siteRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update site with unknown alternative region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteUpdateRequest("New Site $testName", siteCreateResponse)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN"))))
        }
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressRegionNotFound, "IGNORED", siteRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator tries to update the site with too many identifiers
     * THEN operator sees 400 bad request error
     *
     * ToDo:
     *  At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try update site with too many identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteCreateResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteUpdateRequest("New Site $testName", siteCreateResponse)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createAddressIdentifier(testName, it) })))
        }
        val siteResponse = poolClient.sites.updateSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteUpdateError.MainAddressIdentifiersTooMany, "IGNORED", siteRequest.bpns)
        val expectedResponse = SitePartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteUpdate(siteResponse, expectedResponse)
    }

}