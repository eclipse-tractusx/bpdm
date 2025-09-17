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
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SiteCreationIT @Autowired constructor(
    private val poolClient: PoolApiClient,
    private val testDataV6Factory: TestDataV6Factory,
    private val assertRepo: AssertRepositoryV6
): OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator creates a new valid site for legal entity
     * THEN created site returned
     */
    @Test
    fun `create valid site`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val siteRequest = testDataV6Factory.request.createSiteRequest("Site $testName", givenLegalEntity.legalEntity.bpnl)
        val response = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedSite = testDataV6Factory.result.mapLegalAddressSiteToCreateDto(siteRequest, legalEntityRequest.legalEntity.isCatenaXMemberData)
        val expectedResponse = SitePartnerCreateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepo.assertSiteCreate(response, expectedResponse)
    }

    /**
     * GIVEN operator created site
     * WHEN operator searches for created site
     * THEN operator finds created site
     */
    @Test
    fun `create valid site and find it`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity
        val siteRequest = testDataV6Factory.request.createSiteRequest("Site $testName", givenLegalEntity.bpnl)
        val givenSiteResponse = poolClient.sites.createSite(listOf(siteRequest)).entities.single()

        //WHEN
        val response = poolClient.sites.postSiteSearch(SiteSearchRequest(siteBpns = listOf(givenSiteResponse.site.bpns)), PaginationRequest())

        //THEN
        val expectedSites = response.content.map { testDataV6Factory.result.mapToExpectedSite(givenSiteResponse) }
        val expectedResponse = PageDto(1, 1, 0, 1, expectedSites)

        assertRepo.assertSiteSearch(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator creates a valid site for legal entity that has the legal address as site main address
     * THEN created site returned
     */
    @Test
    fun `create valid legal address site`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val siteRequest = testDataV6Factory.request.createLegalAddressSite("Site $testName", givenLegalEntity.legalEntity.bpnl)
        val response = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest))

        //THEN
        val expectedSite = testDataV6Factory.result.mapLegalAddressSiteToCreateDto(siteRequest, givenLegalEntity)
        val expectedResponse = SitePartnerCreateResponseWrapper(listOf(expectedSite), emptyList())

        assertRepo.assertLegalAddressSiteCreate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address site
     * WHEN operator tries to create a new legal address site
     * THEN operator sees error
     *
     * ToDo:
     *  Wrong error response: https://github.com/eclipse-tractusx/bpdm/issues/1471
     */
    @Test
    @Disabled
    fun `try create duplicate legal address site`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()
        val legalAddressSiteRequest = testDataV6Factory.request.createLegalAddressSite("Given Legal Address Site $testName", givenLegalEntity.legalEntity.bpnl)
        poolClient.sites.createSiteWithLegalReference(listOf(legalAddressSiteRequest))

        //WHEN
        val siteRequest = testDataV6Factory.request.createLegalAddressSite("New Legal Address Site $testName", givenLegalEntity.legalEntity.bpnl)
        val response = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.LegalEntityNotFound, "IGNORED", "0")
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalAddressSiteCreate(response, expectedResponse)
    }



    /**
     * GIVEN legal entity and site with main address identifier X
     * WHEN operator tries to create a new site for legal entity with main address identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create site with duplicate address identifier`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity
        val givenSiteRequest = testDataV6Factory.request.createSiteRequest("Given Site $testName", givenLegalEntity.bpnl)
        poolClient.sites.createSite(listOf(givenSiteRequest))
        val identifierX = givenSiteRequest.site.mainAddress.identifiers.first()

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteRequest("New Site $testName", givenLegalEntity.bpnl)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = listOf(identifierX))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))


        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create two new sites for legal entity with the same main address identifier
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create site having duplicate address identifiers`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val siteRequest1 = testDataV6Factory.request.createSiteRequest("Site 1 $testName", givenLegalEntity.bpnl)
        val sameIdentifier = siteRequest1.site.mainAddress.identifiers.first()
        val siteRequest2 = with(testDataV6Factory.request.createSiteRequest("Site 2 $testName", givenLegalEntity.bpnl)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = listOf(sameIdentifier))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest1, siteRequest2))

        //THEN
        val expectedErrors = listOf(siteRequest1, siteRequest2).map { ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown identifier type
     * THEN operator sees identifier not found error
     */
    @Test
    fun `try create site with unknown address identifier`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteRequest("New Site $testName", givenLegalEntity.bpnl)){
            val unknownIdentifier = site.mainAddress.identifiers.first().copy(type = "UNKNOWN")
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = site.mainAddress.identifiers.drop(1).plus(unknownIdentifier))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressIdentifierNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new site for unknown legal entity
     * THEN operator sees legal entity not found error
     */
    @Test
    fun `try create site with unknown legal entity`(){
        //WHEN
        val siteRequest = testDataV6Factory.request.createSiteRequest("New Site $testName", "UNKNOWN")
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.LegalEntityNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create site with unknown physical region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteRequest("New Site $testName", givenLegalEntity.bpnl)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(physicalPostalAddress = site.mainAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN"))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressRegionNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create site with unknown alternative region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteRequest("New Site $testName", givenLegalEntity.bpnl)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(alternativePostalAddress = site.mainAddress.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN"))))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressRegionNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new site for legal entity with too many identifiers
     * THEN operator sees region not found error
     *
     * ToDo:
     *  At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try create site with too many identifiers`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Legal Entity $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val siteRequest = with(testDataV6Factory.request.createSiteRequest("New Site $testName", givenLegalEntity.bpnl)){
            copy(site = site.copy(mainAddress = site.mainAddress.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createAddressIdentifier(testName, it) })))
        }
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest))

        //THEN
        val expectedError = ErrorInfo(SiteCreateError.MainAddressRegionNotFound, "IGNORED", siteRequest.index)
        val expectedResponse = SitePartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertSiteCreate(siteResponse, expectedResponse)
    }

}