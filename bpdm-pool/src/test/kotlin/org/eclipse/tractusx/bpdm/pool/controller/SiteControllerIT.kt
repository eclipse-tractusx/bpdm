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
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteWithReferenceResponse
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
        val searchResult = webTestClient.invokePostEndpoint<PageResponse<SiteWithReferenceResponse>>(EndpointValues.CATENA_SITE_SEARCH_PATH, siteSearchRequest)

        val expectedSiteWithReference1 = SiteWithReferenceResponse(ResponseValues.site1, bpnL1)
        val expectedSiteWithReference2 = SiteWithReferenceResponse(ResponseValues.site2, bpnL1)
        val expectedSiteWithReference3 = SiteWithReferenceResponse(ResponseValues.site3, bpnL2)

        assertThat(searchResult.content)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2, expectedSiteWithReference3))
    }

    private fun requestSite(bpnSite: String) =
        webTestClient.invokeGetEndpoint<SiteWithReferenceResponse>(EndpointValues.CATENA_SITES_PATH + "/${bpnSite}")

    private fun requestSitesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<SiteResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_SITES_PATH_POSTFIX)
}