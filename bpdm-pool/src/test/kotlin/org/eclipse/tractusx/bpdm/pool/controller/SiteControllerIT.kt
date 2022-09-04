/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SiteControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata(webTestClient)
    }

    /**
     * Given partners in db
     * When requesting a site by bpn-s
     * Then site is returned
     */
    @Test
    fun `get site by bpn-s`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1), webTestClient)

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
        testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1), webTestClient)

        webTestClient.get()
            .uri(EndpointValues.CATENA_SITES_PATH + "/NONEXISTENT_BPN")
            .exchange().expectStatus().isNotFound
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
            ),
            webTestClient
        )

        val bpnL1 = createdStructures[0].legalEntity.bpn
        val bpnL2 = createdStructures[1].legalEntity.bpn

        val siteSearchRequest = SiteSearchRequest(listOf(bpnL1, bpnL2))
        val searchResult = webTestClient.invokePostEndpoint<PageResponse<SitePartnerSearchResponse>>(EndpointValues.CATENA_SITE_SEARCH_PATH, siteSearchRequest)

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
        val givenLegalEntities = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        val bpnL1 = givenLegalEntities.first().bpn
        val bpnL2 = givenLegalEntities.last().bpn

        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2, ResponseValues.siteUpsert3)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(legalEntity = bpnL1),
            RequestValues.siteCreate2.copy(legalEntity = bpnL2),
            RequestValues.siteCreate3.copy(legalEntity = bpnL2)
        )
        val response = webTestClient.invokePostWithArrayResponse<SitePartnerCreateResponse>(EndpointValues.CATENA_SITES_PATH, toCreate)

        assertThatCreatedSitesEqual(response, expected)
    }

    /**
     * Given legal entities
     * When creating sites with existing and non-existing bpnl
     * Then only create sites with existing bpnl
     */
    @Test
    fun `don't create sites with non-existing parent`() {
        val givenLegalEntities = webTestClient.invokePostWithArrayResponse<LegalEntityPartnerCreateResponse>(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        val bpnL1 = givenLegalEntities.first().bpn
        val bpnL2 = givenLegalEntities.last().bpn


        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(legalEntity = bpnL1),
            RequestValues.siteCreate2.copy(legalEntity = bpnL2),
            RequestValues.siteCreate3.copy(legalEntity = "NONEXISTENT")
        )
        val response = webTestClient.invokePostWithArrayResponse<SitePartnerCreateResponse>(EndpointValues.CATENA_SITES_PATH, toCreate)

        assertThatCreatedSitesEqual(response, expected)
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
            ),
            webTestClient
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
        val response = webTestClient.invokePutWithArrayResponse<SitePartnerCreateResponse>(EndpointValues.CATENA_SITES_PATH, toUpdate)

        testHelpers.assertRecursively(response).isEqualTo(expected)
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
            ),
            webTestClient
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
        val response = webTestClient.invokePutWithArrayResponse<SitePartnerCreateResponse>(EndpointValues.CATENA_SITES_PATH, toUpdate)

        testHelpers.assertRecursively(response).isEqualTo(expected)
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
            ),
            webTestClient
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { MainAddressSearchResponse(it.site.bpn, it.site.mainAddress) }

        val toSearch = expected.map { it.site }
        val response = webTestClient.invokePostWithArrayResponse<MainAddressSearchResponse>(EndpointValues.CATENA_SITE_MAIN_ADDRESS_SEARCH_PATH, toSearch)

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
            ),
            webTestClient
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { MainAddressSearchResponse(it.site.bpn, it.site.mainAddress) }

        val toSearch = expected.map { it.site }.plus("NON-EXISTENT")
        val response = webTestClient.invokePostWithArrayResponse<MainAddressSearchResponse>(EndpointValues.CATENA_SITE_MAIN_ADDRESS_SEARCH_PATH, toSearch)

        testHelpers.assertRecursively(response).isEqualTo(expected)
    }

    private fun assertThatCreatedSitesEqual(actuals: Collection<SitePartnerCreateResponse>, expected: Collection<SitePartnerCreateResponse>) {
        actuals.forEach { assertThat(it.bpn).matches(testHelpers.bpnSPattern) }

        testHelpers.assertRecursively(actuals).ignoringFields(SitePartnerCreateResponse::bpn.name).isEqualTo(expected)
    }

    private fun requestSite(bpnSite: String) =
        webTestClient.invokeGetEndpoint<SitePartnerSearchResponse>(EndpointValues.CATENA_SITES_PATH + "/${bpnSite}")

    private fun requestSitesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<SitePartnerResponse>>(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpn}" + EndpointValues.CATENA_SITES_PATH_POSTFIX)
}