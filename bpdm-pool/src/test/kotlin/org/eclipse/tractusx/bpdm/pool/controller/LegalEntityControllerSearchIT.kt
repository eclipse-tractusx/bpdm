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

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityMatchResponse
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
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
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

    private lateinit var givenPartner1: LegalEntityVerboseDto
    private lateinit var givenPartner2: LegalEntityVerboseDto
    private lateinit var legalName1: String
    private lateinit var legalName2: String
    private lateinit var legalAddress1: LogisticAddressVerboseDto
    private lateinit var legalAddress2: LogisticAddressVerboseDto

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        //webTestClient.invokeDeleteEndpointWithoutResponse(EndpointValues.OPENSEARCH_SYNC_PATH)
        poolClient.opensearch().clear()
        testHelpers.createTestMetadata()
        val givenStructure = testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2))
        givenPartner1 = with(givenStructure[0].legalEntity) { legalEntity }
        givenPartner2 = with(givenStructure[1].legalEntity) { legalEntity }
        legalName1 = givenStructure[0].legalEntity.legalName
        legalName2 = givenStructure[1].legalEntity.legalName
        legalAddress1 = givenStructure[0].legalEntity.legalAddress
        legalAddress2 = givenStructure[1].legalEntity.legalAddress
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
                LegalEntityMatchResponse(score = 0f, legalEntity = givenPartner1, legalName = legalName1, legalAddress = legalAddress1),
                LegalEntityMatchResponse(score = 0f, legalEntity = givenPartner2, legalName = legalName2, legalAddress = legalAddress2)
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
                LegalEntityMatchResponse(score = 0f, legalEntity = givenPartner1, legalName = legalName1, legalAddress = legalAddress1)
            )
        )
        val expectedSecondPage = PageResponse(
            2, 2, 1, 1, listOf(
                LegalEntityMatchResponse(score = 0f, legalEntity = givenPartner2, legalName = legalName2, legalAddress = legalAddress2)
            )
        )

        val firstPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 0, size = 1)
        val secondPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 1, size = 1)

        assertPageEquals(firstPage, expectedFirstPage)
        assertPageEquals(secondPage, expectedSecondPage)
    }

//    /**
//     * Given partners in OpenSearch
//     * When searching by site name
//     * Then business partner is found
//     */
//    @Test
//    fun `search business partner by site name, result found`() {
//
//        val expected = PageResponse(
//            1, 1, 0, 1, listOf(
//                LegalEntityMatchResponse(score = 0f, legalEntity = givenPartner2, legalName = legalName2, legalAddress = legalAddress2)
//            )
//        )
//
//        val pageResponse = searchBusinessPartnerBySiteName(RequestValues.siteCreate2.site.name, 0, 10)
//
//        assertPageEquals(pageResponse, expected)
//    }

    /**
     * Given partners in OpenSearch
     * When searching by nonexistent site name
     * Then no business partner is found
     */
//    @Test
//    fun `search business partner by site name, no result found`() {
//        val foundPartners = searchBusinessPartnerBySiteName("nonexistent name", 0, 10).content
//        assertThat(foundPartners).isEmpty()
//    }

    private fun searchBusinessPartnerBySiteName(siteName: String, page: Int, size: Int): PageResponse<LegalEntityMatchResponse> {
        val sitePropertiesSearchRequest = SitePropertiesSearchRequest(siteName)

        return poolClient.legalEntities().getLegalEntities(
            LegalEntityPropertiesSearchRequest.EmptySearchRequest,
            PaginationRequest(page, size)
        )


    }

    private fun assertPageEquals(actual: PageResponse<LegalEntityMatchResponse>, expected: PageResponse<LegalEntityMatchResponse>) {
        testHelpers.assertRecursively(actual)
            .ignoringFieldsMatchingRegexes(".*${LegalEntityMatchResponse::score.name}")
            .isEqualTo(expected)
    }
}