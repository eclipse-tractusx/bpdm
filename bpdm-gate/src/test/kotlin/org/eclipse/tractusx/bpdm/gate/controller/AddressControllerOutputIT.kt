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

package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.AugmentedBusinessPartnerResponseCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.CommonValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.GATE_API_OUTPUT_ADDRESSES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_ADDRESSES_SEARCH_PATH
import org.eclipse.tractusx.bpdm.gate.util.ResponseValues
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class AddressControllerOutputIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper
) {
    companion object {
        @RegisterExtension
        private val wireMockServerCdq: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @RegisterExtension
        private val wireMockServerBpdmPool: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServerCdq.baseUrl() }
            registry.add("bpdm.pool.base-url") { wireMockServerBpdmPool.baseUrl() }
        }
    }

    /**
     * Given addresses exists in cdq and bpdm pool
     * When getting addresses page via output route
     * Then addresses page should be returned
     */
    @Test
    fun `get addresses`() {
        val addressesCdq = listOf(
            CdqValues.addressBusinessPartner1,
            CdqValues.addressBusinessPartner2
        )

        val expectedAddresses = listOf(
            ResponseValues.addressGateOutput1,
            ResponseValues.addressGateOutput2
        )

        val addressesPool = listOf(
            ResponseValues.addressPartnerSearchResponse1,
            ResponseValues.addressPartnerSearchResponse2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10

        wireMockServerCdq.stubFor(
            get(urlPathMatching(CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = addressesCdq.map { AugmentedBusinessPartnerResponseCdq(it) }
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PageResponse(
                                    totalElements = addressesPool.size.toLong(),
                                    totalPages = 1,
                                    page = 0,
                                    contentSize = addressesPool.size,
                                    content = addressesPool
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.post()
            .uri { builder ->
                builder.path(GATE_API_OUTPUT_ADDRESSES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<AddressGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = 0
            )
        )
    }

    /**
     * Given addresses exists in cdq and bpdm pool
     * When getting addresses page via output route filtering by external ids
     * Then addresses page should be returned
     */
    @Test
    fun `get addresses, filter by external ids`() {
        val addressesCdq = listOf(
            CdqValues.addressBusinessPartner1,
            CdqValues.addressBusinessPartner2
        )

        val expectedAddresses = listOf(
            ResponseValues.addressGateOutput1,
            ResponseValues.addressGateOutput2
        )

        val addressesPool = listOf(
            ResponseValues.addressPartnerSearchResponse1,
            ResponseValues.addressPartnerSearchResponse2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10

        wireMockServerCdq.stubFor(
            get(urlPathMatching(CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalIds", equalTo(listOf(CommonValues.externalIdAddress1, CommonValues.externalIdAddress2).joinToString(",")))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = addressesCdq.map { AugmentedBusinessPartnerResponseCdq(it) }
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PageResponse(
                                    totalElements = addressesPool.size.toLong(),
                                    totalPages = 1,
                                    page = 0,
                                    contentSize = addressesPool.size,
                                    content = addressesPool
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.post()
            .uri { builder ->
                builder.path(GATE_API_OUTPUT_ADDRESSES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(listOf(CommonValues.externalIdAddress1, CommonValues.externalIdAddress2)))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<AddressGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = 0
            )
        )
    }
}