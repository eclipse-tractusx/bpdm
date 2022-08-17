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
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SitePropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
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
class BusinessPartnerControllerSearchIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    //Remove identifiers to be able to insert same legal entity values several times
    private val legalEntityWithoutIdentifiers = with(RequestValues.legalEntityCreate1) { copy(properties = properties.copy(identifiers = emptyList())) }
    private val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = legalEntityWithoutIdentifiers,
        siteStructures = listOf(
            SiteStructureRequest(RequestValues.siteCreate1),
            SiteStructureRequest(RequestValues.siteCreate2)
        )
    )
    private val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(RequestValues.siteCreate3)
        )
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        webTestClient.invokeDeleteEndpointWithoutResponse(EndpointValues.OPENSEARCH_SYNC_PATH)

        testHelpers.createTestMetadata(webTestClient)
        testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2), webTestClient)
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting page with multiple items
     * Then response contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple items in page`() {
        // insert partner again, so we get multiple search results
        testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1), webTestClient)
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        val pageResponse = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 0, size = 100)
        assertThat(pageResponse.contentSize).isEqualTo(2)
        assertThat(pageResponse.page).isEqualTo(0)
        assertThat(pageResponse.totalElements).isEqualTo(2)
        assertThat(pageResponse.totalPages).isEqualTo(1)
    }

    /**
     * Given partners with same siteName in OpenSearch
     * When searching by site name and requesting multiple pages
     * Then responses contains correct pagination values
     */
    @Test
    fun `search business partner with pagination, multiple pages`() {
        // insert partner again, so we get multiple search results
        testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1), webTestClient)
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        val firstPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 0, size = 1)
        assertThat(firstPage.contentSize).isEqualTo(1)
        assertThat(firstPage.page).isEqualTo(0)
        assertThat(firstPage.totalElements).isEqualTo(2)
        assertThat(firstPage.totalPages).isEqualTo(2)

        val secondPage = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name, page = 1, size = 1)
        assertThat(secondPage.contentSize).isEqualTo(1)
        assertThat(secondPage.page).isEqualTo(1)
        assertThat(secondPage.totalElements).isEqualTo(2)
        assertThat(secondPage.totalPages).isEqualTo(2)
    }

    /**
     * Given partners in OpenSearch
     * When searching by site name
     * Then business partner is found
     */
    @Test
    fun `search business partner by site name, result found`() {
        val foundPartners = searchBusinessPartnerBySiteName(RequestValues.siteCreate1.site.name).content
        assertThat(foundPartners).hasSize(1)
        assertThat(foundPartners.single().legalEntity.properties.names.first()).isEqualTo(ResponseValues.legalEntity1.properties.names.first())
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

    private fun searchBusinessPartnerBySiteName(siteName: String, page: Int? = null, size: Int? = null): PageResponse<BusinessPartnerSearchResponse> {
        return webTestClient.invokeGetEndpoint(
            EndpointValues.CATENA_BUSINESS_PARTNER_PATH,
            *(listOfNotNull(
                SitePropertiesSearchRequest::siteName.name to siteName,
                if (page != null) PaginationRequest::page.name to page.toString() else null,
                if (size != null) PaginationRequest::size.name to size.toString() else null
            ).toTypedArray())
        )
    }
}