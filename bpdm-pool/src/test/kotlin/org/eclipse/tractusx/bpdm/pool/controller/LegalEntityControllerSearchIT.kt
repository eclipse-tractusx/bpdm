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
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for the search endpoint of the business partner controller
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class LegalEntityControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    private val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate1,
        siteStructures = listOf(
            SiteStructureRequest(RequestValues.siteCreate1)
        )
    )
    private val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(RequestValues.siteCreate2),
            SiteStructureRequest(RequestValues.siteCreate1) //same site here to attain multiple results when needed
        )
    )

    private lateinit var givenPartner1: LegalEntityPartnerResponse
    private lateinit var givenPartner2: LegalEntityPartnerResponse

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        webTestClient.invokeDeleteEndpointWithoutResponse(EndpointValues.OPENSEARCH_SYNC_PATH)

        testHelpers.createTestMetadata(webTestClient)
        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2), webTestClient)
        givenPartner1 = with(givenStructure[0].legalEntity) { LegalEntityPartnerResponse(bpn, properties, currentness) }
        givenPartner2 = with(givenStructure[1].legalEntity) { LegalEntityPartnerResponse(bpn, properties, currentness) }

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting page with multiple items
     * Then response contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple items in page`() {

        val expected = PageResponse(
            2, 1, 0, 2,
            listOf(
                LegalEntityMatchResponse(0f, givenPartner1),
                LegalEntityMatchResponse(0f, givenPartner2)
            )
        )

        val pageResponse = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 0, size = 100)

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting multiple pages
     * Then responses contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple pages`() {

        val expectedFirstPage = PageResponse(
            2, 2, 0, 1, listOf(
                LegalEntityMatchResponse(0f, givenPartner1)
            )
        )
        val expectedSecondPage = PageResponse(
            2, 2, 1, 1, listOf(
                LegalEntityMatchResponse(0f, givenPartner2)
            )
        )

        val firstPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 0, size = 1)
        val secondPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 1, size = 1)

        assertPageEquals(firstPage, expectedFirstPage)
        assertPageEquals(secondPage, expectedSecondPage)
    }

    /**
     * Given partners in OpenSearch
     * When searching by site name
     * Then business partner is found
     */
    @Test
    fun `search business partner by site name, result found`() {

        val expected = PageResponse(
            1, 1, 0, 1, listOf(
                LegalEntityMatchResponse(0f, givenPartner2)
            )
        )

        val pageResponse = searchBusinessPartnerBySiteName(RequestValues.siteCreate2.site.name)

        assertPageEquals(pageResponse, expected)
    }

    /**
     * Given partners in OpenSearch
     * When searching by nonexistent site name
     * Then no business partner is found
     */
    @Test
    fun `search business partner by site name, no result found`() {
        val foundPartners = searchBusinessPartnerBySiteName("nonexistent name").content
        assertThat(foundPartners).isEmpty()
    }

    private fun searchBusinessPartnerBySiteName(siteName: String, page: Int? = null, size: Int? = null): PageResponse<LegalEntityMatchResponse> {
        return webTestClient.invokeGetEndpoint(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            *(listOfNotNull(
                SitePropertiesSearchRequest::siteName.name to siteName,
                if (page != null) PaginationRequest::page.name to page.toString() else null,
                if (size != null) PaginationRequest::size.name to size.toString() else null
            ).toTypedArray())
        )
    }

    private fun assertPageEquals(actual: PageResponse<LegalEntityMatchResponse>, expected: PageResponse<LegalEntityMatchResponse>) {
        testHelpers.assertRecursively(actual)
            .ignoringFieldsMatchingRegexes(".*${LegalEntityMatchResponse::score.name}")
            .isEqualTo(expected)
    }
}