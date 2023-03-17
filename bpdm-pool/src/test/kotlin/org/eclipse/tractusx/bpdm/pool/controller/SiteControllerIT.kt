/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SiteControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
    }

    /**
     * Given partners in db
     * When requesting a site by bpn-s
     * Then site is returned
     */
    @Test
    fun `get site by bpn-s`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1))

        val importedPartner = createdStructures.single().legalEntity
        importedPartner.bpn
            .let { bpn -> requestSitesOfLegalEntity(bpn).content.single().bpn }
            .let { bpnSite -> requestSite(bpnSite) }
            .let { siteResponse ->
                assertThat(siteResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }

    /**
     * Given partners in db
     * When requesting an site by non-existent bpn-s
     * Then a "not found" response is sent
     */
    @Test
    fun `get site by bpn-s, not found`() {
        testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1))
        testHelpers.`get site by bpn-s, not found`("NONEXISTENT_BPN")
    }

    /**
     * Given sites
     * When searching for sites via BPNS
     * Then return those sites
     */
    @Test
    fun `search sites by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = RequestValues.siteCreate1),
                        SiteStructureRequest(site = RequestValues.siteCreate2),
                        SiteStructureRequest(site = RequestValues.siteCreate3)
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.bpn
        val bpnS2 = createdStructures[0].siteStructures[1].site.bpn
        val bpnL = createdStructures[0].legalEntity.bpn

        val siteSearchRequest = SiteBpnSearchRequest(emptyList(), listOf(bpnS1, bpnS2))
        val searchResult = poolClient.sites().searchSites(siteSearchRequest, PaginationRequest())
        val expectedSiteWithReference1 = SitePartnerSearchResponse(ResponseValues.site1, bpnL)
        val expectedSiteWithReference2 = SitePartnerSearchResponse(ResponseValues.site2, bpnL)

        testHelpers.assertRecursively(searchResult.content)
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2))
    }

    /**
     * Given sites of business partners
     * When searching for sites via BPNL
     * Then return sites that belong to those legal entities
     */
    @Test
    fun `search sites by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = RequestValues.siteCreate1),
                        SiteStructureRequest(site = RequestValues.siteCreate2),
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(site = RequestValues.siteCreate3))
                )
            )
        )

        val bpnL1 = createdStructures[0].legalEntity.bpn
        val bpnL2 = createdStructures[1].legalEntity.bpn

        val siteSearchRequest = SiteBpnSearchRequest(listOf(bpnL1, bpnL2))
        val searchResult = poolClient.sites().searchSites(siteSearchRequest, PaginationRequest())
        val expectedSiteWithReference1 = SitePartnerSearchResponse(ResponseValues.site1, bpnL1)
        val expectedSiteWithReference2 = SitePartnerSearchResponse(ResponseValues.site2, bpnL1)
        val expectedSiteWithReference3 = SitePartnerSearchResponse(ResponseValues.site3, bpnL2)

        testHelpers.assertRecursively(searchResult.content)
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2, expectedSiteWithReference3))
    }


    /**
     * Given legal entities
     * When requesting new sites for legal entities
     * Then new sites with BPN returned
     */
    @Test
    fun `create new sites`() {

        val givenLegalEntities = poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().bpn
        val bpnL2 = givenLegalEntities.last().bpn

        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2, ResponseValues.siteUpsert3)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(legalEntity = bpnL1),
            RequestValues.siteCreate2.copy(legalEntity = bpnL2),
            RequestValues.siteCreate3.copy(legalEntity = bpnL2)
        )

        val response = poolClient.sites().createSite(toCreate)

        assertThatCreatedSitesEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given legal entities
     * When creating sites with existing and non-existing bpnl
     * Then only create sites with existing bpnl
     */
    @Test
    fun `don't create sites with non-existing parent`() {


        val givenLegalEntities = poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().bpn
        val bpnL2 = givenLegalEntities.last().bpn


        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(legalEntity = bpnL1),
            RequestValues.siteCreate2.copy(legalEntity = bpnL2),
            RequestValues.siteCreate3.copy(legalEntity = "NONEXISTENT")
        )
        val response = poolClient.sites().createSite(toCreate)

        // 2 entities okay
        assertThatCreatedSitesEqual(response.entities, expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteCreateError.LegalEntityNotFound, CommonValues.index3)
    }

    /**
     * Given sites
     * When updating sites via BPN
     * Then update those sites
     */
    @Test
    fun `update existing sites`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.bpn
        val bpnS2 = givenStructure[1].siteStructures[0].site.bpn
        val bpnS3 = givenStructure[1].siteStructures[1].site.bpn

        val expected = listOf(
            ResponseValues.siteUpsert1.copy(bpn = bpnS3, index = null),
            ResponseValues.siteUpsert2.copy(bpn = bpnS1, index = null),
            ResponseValues.siteUpsert3.copy(bpn = bpnS2, index = null)
        )

        val toUpdate = listOf(
            RequestValues.siteUpdate1.copy(bpn = bpnS3),
            RequestValues.siteUpdate2.copy(bpn = bpnS1),
            RequestValues.siteUpdate3.copy(bpn = bpnS2)
        )

        val response = poolClient.sites().updateSite(toUpdate)

        testHelpers.assertRecursively(response.entities).isEqualTo(expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given sites
     * When updating via existent and non-existent BPNs
     * Then only update sites with existent BPN
     */
    @Test
    fun `ignore non-existent BPNS updates`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1), SiteStructureRequest(RequestValues.siteCreate2))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.bpn
        val bpnS2 = givenStructure[0].siteStructures[1].site.bpn

        val expected = listOf(
            ResponseValues.siteUpsert1.copy(bpn = bpnS2, index = null),
            ResponseValues.siteUpsert2.copy(bpn = bpnS1, index = null)
        )

        val toUpdate = listOf(
            RequestValues.siteUpdate1.copy(bpn = bpnS2),
            RequestValues.siteUpdate2.copy(bpn = bpnS1),
            RequestValues.siteUpdate3.copy(bpn = "NONEXISTENT"),
        )
        val response = poolClient.sites().updateSite(toUpdate)

        // 2 entities okay
        testHelpers.assertRecursively(response.entities).isEqualTo(expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteUpdateError.SiteNotFound, "NONEXISTENT")
    }

    /**
     * Given sites
     * When asking for main addresses by site BPNs
     * Then main addresses of sites returned
     */
    @Test
    fun `find main addresses by BPNS`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { MainAddressSearchResponse(it.site.bpn, it.site.mainAddress) }

        val toSearch = expected.map { it.site }

        val response = poolClient.sites().searchMainAddresses(toSearch)
        testHelpers.assertRecursively(response).isEqualTo(expected)
    }

    /**
     * Given sites
     * When asking for main addresses with non-existent BPNs
     * Then only main addresses of sites with existing BPNs returned
     */
    @Test
    fun `find main address, ignore invalid BPNS`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { MainAddressSearchResponse(it.site.bpn, it.site.mainAddress) }

        val toSearch = expected.map { it.site }.plus("NON-EXISTENT")

        val response = poolClient.sites().searchMainAddresses(toSearch)
        testHelpers.assertRecursively(response).isEqualTo(expected)
    }

    private fun assertThatCreatedSitesEqual(actuals: Collection<SitePartnerCreateResponse>, expected: Collection<SitePartnerCreateResponse>) {
        actuals.forEach { assertThat(it.bpn).matches(testHelpers.bpnSPattern) }

        testHelpers.assertRecursively(actuals).ignoringFields(SitePartnerCreateResponse::bpn.name).isEqualTo(expected)
    }

    private fun requestSite(bpnSite: String) = poolClient.sites().getSite(bpnSite)


    private fun requestSitesOfLegalEntity(bpn: String) = poolClient.legalEntities().getSites(bpn, PaginationRequest())

}