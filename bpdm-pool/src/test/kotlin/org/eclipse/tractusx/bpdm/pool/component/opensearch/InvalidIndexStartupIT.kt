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

package org.eclipse.tractusx.bpdm.pool.component.opensearch

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LEGAL_ENTITIES_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.RegisterExtension
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
@TestMethodOrder(OrderAnnotation::class)
class InvalidIndexStartupIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val testHelpers: TestHelpers,
    private val openSearchClient: OpenSearchClient,
    private val poolClient: PoolClientImpl
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

    }

    val bpnBogusDocument = "BPN_FAKE"

    /**
     * Not a real test but prepares the OpenSearch container for the next test that will be run in a fresh Spring-Boot context
     * Create an invalid OpenSearch index and fill it with bogus content
     */
    @Test
    @Order(0)
    @DirtiesContext
    fun setupIndexForNextTest() {
        testHelpers.truncateDbTables()
        //Clear and set up an invalid OpenSearch context
        openSearchClient.indices().delete { it.index(LEGAL_ENTITIES_INDEX_NAME) }
        openSearchClient.indices().create { createRequestIndex ->
            createRequestIndex.index(LEGAL_ENTITIES_INDEX_NAME).mappings { mapping ->
                mapping.properties("outdatedField") { property ->
                    property.searchAsYouType { it }
                }
            }
        }

        //Create a bogus document with a valid BPN
        val invalidBp = InvalidBusinessPartnerDoc("outdated")

        openSearchClient.index { indexRequest ->
            indexRequest.index(LEGAL_ENTITIES_INDEX_NAME).id(bpnBogusDocument).document(invalidBp).refresh(Refresh.True)
        }

        //Check whether it really is inside the index
        val getResponse =
            openSearchClient.get({ getRequest -> getRequest.index(LEGAL_ENTITIES_INDEX_NAME).id(bpnBogusDocument) }, InvalidBusinessPartnerDoc::class.java)
        assertThat(getResponse.found()).isTrue
    }

    /**
     * Given non-empty OpenSearch index with outdated/invalid document structure
     * When application starts
     * Then index deleted and recreated with up-to-date document structure
     */
    @Test
    @Order(1)
    fun recreateOutdatedIndexOnStartup() {
        // bogus document should not be in index anymore
        val getResponse =
            openSearchClient.get({ getRequest -> getRequest.index(LEGAL_ENTITIES_INDEX_NAME).id(bpnBogusDocument) }, InvalidBusinessPartnerDoc::class.java)
        assertThat(getResponse.found()).isFalse

        //import a business partner to DB
        testHelpers.createTestMetadata()
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                )
            )
        )
        //export to index and check whether the imported business partner can be found as normal
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.OPENSEARCH_SYNC_PATH)

        val searchResult = poolClient.legalEntities().getLegalEntities(
            LegalEntityPropertiesSearchRequest.EmptySearchRequest,
            PaginationRequest()
        )

        assertThat(searchResult.content).isNotEmpty
        assertThat(searchResult.contentSize).isEqualTo(1)
    }

    private data class InvalidBusinessPartnerDoc(
        val outdatedField: String = ""
    )
}