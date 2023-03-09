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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl

import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service.OpenSearchSyncStarterService
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.util.stream.Stream

/**
 * Integration tests for the look-ahead endpoints of the business partner controller
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles(value = ["test"])
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
@AutoConfigureWebTestClient(timeout = "10000")
class SuggestionControllerIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val openSearchSyncService: OpenSearchSyncStarterService,
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        private val expectedLegalEntity = ResponseValues.legalEntityUpsert1.properties
        private val expectedLegalEntityName = expectedLegalEntity.legalName.value
        private val expectedLegalAddress = ResponseValues.legalEntityUpsert1.legalAddress
        private val expectedSite = ResponseValues.siteUpsert2

        private val nonlatinLegalEntity = ResponseValues.legalEntityUpsert3.properties
        private val nonlatinLegalAddress = ResponseValues.legalEntityUpsert3.legalAddress
        private val nonlatinSite = ResponseValues.siteUpsert3

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }

        @JvmStatic
        fun argumentsSuggestPropertyValues(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    expectedLegalEntityName,
                    EndpointValues.CATENA_SUGGESTION_LE_NAME_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalEntity.legalForm!!.name,
                    EndpointValues.CATENA_SUGGESTION_LE_LEGAL_FORM_PATH,
                    expectedLegalEntity.legalName.value
                ),
                Arguments.of(
                    expectedLegalEntity.status.first().officialDenotation,
                    EndpointValues.CATENA_SUGGESTION_LE_STATUS_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalEntity.classifications.first().value,
                    EndpointValues.CATENA_SUGGESTION_LE_CLASSIFICATION_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedSite.name,
                    EndpointValues.CATENA_SUGGESTION_SITE_NAME_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.administrativeAreas.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_ADMIN_AREA_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.postCodes.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_POST_CODE_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.localities.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_LOCALITY_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.thoroughfares.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_THOROUGHFARE_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.premises.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_PREMISE_PATH,
                    expectedLegalEntityName
                ),
                Arguments.of(
                    expectedLegalAddress.postalDeliveryPoints.first().value,
                    EndpointValues.CATENA_SUGGESTION_ADDRESS_POSTAL_DELIVERY_POINT_PATH,
                    expectedLegalEntityName
                )
            )

        @JvmStatic
        fun argumentsSuggestPropertyValuesNonLatin(): Stream<Arguments> =
            Stream.of(
                Arguments.of(nonlatinLegalEntity.legalName.value, EndpointValues.CATENA_SUGGESTION_LE_NAME_PATH),
                Arguments.of(nonlatinLegalEntity.legalForm!!.name, EndpointValues.CATENA_SUGGESTION_LE_LEGAL_FORM_PATH),
                Arguments.of(nonlatinSite.name, EndpointValues.CATENA_SUGGESTION_SITE_NAME_PATH),
                Arguments.of(nonlatinLegalAddress.administrativeAreas.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_ADMIN_AREA_PATH),
                Arguments.of(nonlatinLegalAddress.postCodes.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_POST_CODE_PATH),
                Arguments.of(nonlatinLegalAddress.localities.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_LOCALITY_PATH),
                Arguments.of(nonlatinLegalAddress.thoroughfares.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_THOROUGHFARE_PATH),
                Arguments.of(nonlatinLegalAddress.premises.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_PREMISE_PATH),
                Arguments.of(nonlatinLegalAddress.postalDeliveryPoints.first().value, EndpointValues.CATENA_SUGGESTION_ADDRESS_POSTAL_DELIVERY_POINT_PATH)
            )
    }

    val partnerStructure1 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate1,
        siteStructures = listOf(
            SiteStructureRequest(
                site = RequestValues.siteCreate2,
                addresses = listOf(RequestValues.addressPartnerCreate3)
            )
        )
    )

    val partnerStructure2 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate2,
        siteStructures = listOf(
            SiteStructureRequest(
                site = RequestValues.siteCreate3,
                addresses = listOf(RequestValues.addressPartnerCreate1)
            )
        )
    )

    val partnerStructure3 = LegalEntityStructureRequest(
        legalEntity = RequestValues.legalEntityCreate3,
        siteStructures = listOf(
            SiteStructureRequest(
                site = RequestValues.siteCreate1,
                addresses = listOf(RequestValues.addressPartnerCreate2)
            )
        )
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        openSearchSyncService.clearOpenSearch()

        testHelpers.createTestMetadata()
        testHelpers.createBusinessPartnerStructure(listOf(partnerStructure1, partnerStructure2, partnerStructure3))
        openSearchSyncService.export()
    }

    /**
     * Given partner with property value
     * When asking for a suggestion for that property
     * Then show that property value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest property values`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get().uri(endpointPath)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!



        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When asking for a suggestion for that property value
     * Then show that property value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by phrase`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When ask suggestion for a prefix of that value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by prefix`(expectedSuggestionValue: String, endpointPath: String) {

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue.substring(0, 1))
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value that is several words
     * When ask suggestion for a word in value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by word`(expectedSuggestionValue: String, endpointPath: String) {
        val queryText = expectedSuggestionValue.split("\\s".toRegex()).first()

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value
     * When ask suggestion for text that doesn't have a word or prefix in value
     * Then don't show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `don't suggest by different`(expectedSuggestionValue: String, endpointPath: String) {
        val queryText = "xxxxxxDoesntMatchxxxxxx"

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, queryText)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion property 1 with filter by property 2 value
     * Then show property 1 value 1
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest filtered suggestions`(expectedSuggestionValue: String, endpointPath: String, filterName: String) {
        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(LegalEntityPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion for a word in property 1 value with filter by property 2 value
     * Then show property 1 value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `suggest by word in filtered suggestions`(expectedSuggestionValue: String, endpointPath: String, filterName: String) {
        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .queryParam(LegalEntityPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property 1 value and property 2 value
     * When ask suggestion for a word in property 1 value with filter by other than property 2 value
     * Then don't show property 1 value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValues")
    fun `don't suggest by word when filtered out`(expectedSuggestionValue: String, endpointPath: String) {
        val filterName = SaasValues.legalEntity2.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(endpointPath)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedSuggestionValue)
                    .queryParam(LegalEntityPropertiesSearchRequest::name.name, filterName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).noneMatch { it.suggestion == expectedSuggestionValue }
    }

    /**
     * Given partner with property value in non-latin characters
     * When ask suggestion for that value
     * Then show that value
     */
    @ParameterizedTest
    @MethodSource("argumentsSuggestPropertyValuesNonLatin")
    fun `suggest by non-latin characters`(expectedSuggestionValue: String, endpointPath: String) {
        val expectedName = SaasValues.legalEntity3.names.first().value

        val page = webTestClient.get()
            .uri { builder ->
                builder.path(EndpointValues.CATENA_SUGGESTION_LE_NAME_PATH)
                    .queryParam(EndpointValues.TEXT_PARAM_NAME, expectedName)
                    .build()
            }
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<SuggestionResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(page.content).anyMatch { it.suggestion == expectedName }
    }
}