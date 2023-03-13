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
import org.eclipse.tractusx.bpdm.common.dto.saas.AugmentedBusinessPartnerResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.PagedResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.dto.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError
import org.eclipse.tractusx.bpdm.gate.util.SaasValues
import org.eclipse.tractusx.bpdm.gate.util.CommonValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.GATE_API_OUTPUT_SITES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_SITES_SEARCH_PATH
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
internal class SiteControllerOutputIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper
) {
    companion object {
        @RegisterExtension
        private val wireMockServerSaas: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @RegisterExtension
        private val wireMockServerBpdmPool: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServerSaas.baseUrl() }
            registry.add("bpdm.pool.base-url") { wireMockServerBpdmPool.baseUrl() }
        }
    }

    /**
     * Given sites exists in SaaS and bpdm pool
     * When getting sites page via output route
     * Then sites page should be returned
     */
    @Test
    fun `get sites`() {
        val expectedSites = listOf(
            ResponseValues.siteGateOutput1,
            ResponseValues.siteGateOutput2
        )
        val expectedErrors = listOf(
            ErrorInfo(BusinessPartnerOutputError.BpnNotInPool, "BPNS0000000003X9 not found in pool", SaasValues.legalEntityAugmentedNotInPoolResponse.externalId),
            ErrorInfo(BusinessPartnerOutputError.SharingProcessError, "SaaS sharing process error: Error message", SaasValues.legalEntityAugmentedSharingErrorResponse.externalId),
        )
        val expectedPending = listOf(SaasValues.legalEntityAugmentedPendingResponse.externalId!!)

        val sitesSaas = listOf(
            SaasValues.siteBusinessPartner1,
            SaasValues.siteBusinessPartner2,
            SaasValues.siteAugmentedNotInPoolResponse,
            SaasValues.siteAugmentedSharingErrorResponse,
            SaasValues.siteAugmentedPendingResponse,
        )

        val sitesPool = listOf(
            ResponseValues.sitePartnerSearchResponse1,
            ResponseValues.sitePartnerSearchResponse2
        )
        val mainAddressesPool = listOf(
            ResponseValues.mainAddressSearchResponse1,
            ResponseValues.mainAddressSearchResponse2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10

        wireMockServerSaas.stubFor(
            get(urlPathMatching(SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesSaas.map { AugmentedBusinessPartnerResponseSaas(it) }
                                )
                            )
                        )
                )
        )

        wireMockServerSaas.stubFor(
            get(urlPathMatching(EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesSaas
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_SITES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PageResponse(
                                    totalElements = sitesPool.size.toLong(),
                                    totalPages = 1,
                                    page = 0,
                                    contentSize = sitesPool.size,
                                    content = sitesPool
                                )

                            )
                        )
                )
        )
        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(mainAddressesPool)
                        )
                )
        )

        val pageResponse = webTestClient.post()
            .uri { builder ->
                builder.path(GATE_API_OUTPUT_SITES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageOutputResponse<SiteGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageOutputResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedSites,
                invalidEntries = expectedPending.size + expectedErrors.size,
                pending = expectedPending,
                errors = expectedErrors,
            )
        )
    }

    /**
     * Given sites exists in SaaS and bpdm pool
     * When getting sites page via output route filtering by external ids
     * Then sites page should be returned
     */
    @Test
    fun `get sites, filter by external ids`() {
        val expectedSites = listOf(
            ResponseValues.siteGateOutput1,
            ResponseValues.siteGateOutput2
        )

        val sitesSaas = listOf(
            SaasValues.siteBusinessPartner1,
            SaasValues.siteBusinessPartner2
        )

        val sitesPool = listOf(
            ResponseValues.sitePartnerSearchResponse1,
            ResponseValues.sitePartnerSearchResponse2
        )
        val mainAddressesPool = listOf(
            ResponseValues.mainAddressSearchResponse1,
            ResponseValues.mainAddressSearchResponse2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10

        wireMockServerSaas.stubFor(
            get(urlPathMatching(SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalIds", equalTo(listOf(CommonValues.externalIdSite1, CommonValues.externalIdSite2).joinToString(",")))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesSaas.map { AugmentedBusinessPartnerResponseSaas(it) }
                                )
                            )
                        )
                )
        )

        wireMockServerSaas.stubFor(
            get(urlPathMatching(EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesSaas
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_SITES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PageResponse(
                                    totalElements = sitesPool.size.toLong(),
                                    totalPages = 1,
                                    page = 0,
                                    contentSize = sitesPool.size,
                                    content = sitesPool
                                )
                            )
                        )
                )
        )
        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(mainAddressesPool)
                        )
                )
        )

        val pageResponse = webTestClient.post()
            .uri { builder ->
                builder.path(GATE_API_OUTPUT_SITES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(listOf(CommonValues.externalIdSite1, CommonValues.externalIdSite2)))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<SiteGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedSites,
                invalidEntries = 0
            )
        )
    }
}