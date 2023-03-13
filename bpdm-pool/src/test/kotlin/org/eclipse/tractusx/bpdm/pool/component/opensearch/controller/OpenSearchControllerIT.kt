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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.saas.PagedResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.saas.config.SaasAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.saas.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityMatchResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult


/**
 * Integration tests for the data sync endpoints in the OpenSearchController
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class OpenSearchControllerIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val importService: ImportStarterService,
    private val openSearchSyncService: OpenSearchSyncStarterService,
    private val saasAdapterConfigProperties: SaasAdapterConfigProperties,
    private val objectMapper: ObjectMapper,
    private val testHelpers: TestHelpers
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }

    val partnerDocs = listOf(
        SaasValues.legalEntity1,
        SaasValues.legalEntity2,
        SaasValues.legalEntity3
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        openSearchSyncService.clearOpenSearch()

        val importCollection = PagedResponseSaas(
            partnerDocs.size,
            null,
            null,
            partnerDocs.size,
            partnerDocs
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(importCollection))
                )
        )

        importService.import()
    }


    /**
     * Given partners in database already exported
     * When export
     * Then partners are not exported to OpenSearch
     */
    @Test
    fun `export only new partners`() {
        //export once to get partners into opensearch for given system state
        var exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })

        //export now to check behaviour
        exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(0)
    }

    /**
     * Given new partners in database
     * When export
     * Then new partners can be searched
     */
    @Test
    fun `can search exported partners`() {
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })
    }

    /**
     * Given partners in OpenSearch
     * When delete index
     * Then partners can't be searched anymore
     */
    @Test
    fun `empty index`() {
        val names = partnerDocs.map { it.names.first().value }

        // fill the opensearch index
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(names)

        //clear the index
        webTestClient.delete().uri(EndpointValues.OPENSEARCH_SYNC_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //check that the partners can really not be searched anymore
        names.forEach { assertThat(searchBusinessPartnerByName(it)).matches { it.contentSize == 0 } }
    }

    /**
     * Given partners in OpenSearch
     * When delete index and export
     * Then partners again in OpenSearch
     */
    @Test
    fun `export all partners after empty index`() {

        // fill the opensearch index
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)


        //clear the index
        webTestClient.delete().uri(EndpointValues.OPENSEARCH_SYNC_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful

        //export partners again
        val exportResponse = testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        assertThat(exportResponse.count).isEqualTo(3)
        assertSearchableByNames(partnerDocs.map { it.names.first().value })

    }

    private fun searchBusinessPartnerByName(name: String): PageResponse<LegalEntityMatchResponse> {
        return webTestClient.get().uri { builder ->
            builder.path(EndpointValues.CATENA_LEGAL_ENTITY_PATH)
                .queryParam(LegalEntityPropertiesSearchRequest::name.name, name)
                .build()
        }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<LegalEntityMatchResponse>>()
            .responseBody
            .blockFirst()!!
    }

    private fun assertSearchableByNames(names: Collection<String>) {
        names.forEach { name ->
            val pageResult = searchBusinessPartnerByName(name)

            assertThat(pageResult.content).isNotEmpty
            assertThat(pageResult.content.first()).matches { it.legalEntity.properties.names.any { n -> n.value == name } }
        }
    }


}