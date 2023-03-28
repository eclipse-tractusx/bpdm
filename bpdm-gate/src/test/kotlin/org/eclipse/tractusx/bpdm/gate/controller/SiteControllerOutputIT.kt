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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.AugmentedBusinessPartnerResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.PagedResponseSaas
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerOutputError
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_SITES_SEARCH_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class SiteControllerOutputIT @Autowired constructor(
    private val objectMapper: ObjectMapper,
    val gateClient: GateClient
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
            ErrorInfo(BusinessPartnerOutputError.BpnNotInPool, "BPNS0000000003X9 not found in pool", SaasValues.siteNotInPoolResponse.externalId),
            ErrorInfo(BusinessPartnerOutputError.SharingProcessError, "SaaS sharing process error: Error message", SaasValues.siteSharingErrorResponse.externalId),
        )
        val expectedPending = listOf(SaasValues.sitePendingResponse.externalId!!)

        val sitesSaas = listOf(
            SaasValues.siteBusinessPartner1,
            SaasValues.siteBusinessPartner2,
            SaasValues.siteNotInPoolResponse,
            SaasValues.siteSharingErrorResponse,
            SaasValues.sitePendingResponse,
        )

        val sitesPool = listOf(
            ResponseValues.siteResponse1,
            ResponseValues.siteResponse2
        )
        val mainAddressesPool = listOf(
            ResponseValues.logisticAddress1,
            ResponseValues.logisticAddress2
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
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponseValue = gateClient.sites().getSitesOutput(paginationValue, emptyList())

        assertThat(pageResponseValue).isEqualTo(
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
            ResponseValues.siteResponse1,
            ResponseValues.siteResponse2
        )
        val mainAddressesPool = listOf(
            ResponseValues.logisticAddress1,
            ResponseValues.logisticAddress2
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
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponseValue = gateClient.sites().getSitesOutput(paginationValue, listOf(CommonValues.externalIdSite1, CommonValues.externalIdSite2))

        assertThat(pageResponseValue).isEqualTo(
            PageOutputResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedSites,
                invalidEntries = 0,
                pending = listOf(),
                errors = listOf(),
            )
        )
    }
}