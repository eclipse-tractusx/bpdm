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
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.saas.AugmentedBusinessPartnerResponseSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.PagedResponseSaas
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_LEGAL_ADDRESSES_SEARCH_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.POOL_API_MOCK_LEGAL_ENTITIES_SEARCH_PATH
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
internal class LegalEntityControllerOutputIT @Autowired constructor(
    private val objectMapper: ObjectMapper,
    val gateClient: GateClient,
    private val testHelpers: DbTestHelpers
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
     * Given legal entities exists in SaaS and bpdm pool
     * When getting legal entities page via output route
     * Then legal entities page should be returned
     */
    @Test
    fun `get legal entities`() {
        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutput1,
            ResponseValues.legalEntityGateOutput2
        )
        val expectedErrors = listOf(
            ErrorInfo(
                BusinessPartnerSharingError.BpnNotInPool,
                "BPNL0000000002XY not found in pool",
                SaasValues.legalEntityAugmentedNotInPoolResponse.externalId
            ),
            ErrorInfo(
                BusinessPartnerSharingError.SharingProcessError,
                "SaaS sharing process error: Error message",
                SaasValues.legalEntityAugmentedSharingErrorResponse.externalId
            ),
        )
        val expectedPending = listOf(SaasValues.legalEntityAugmentedPendingResponse.externalId!!)

        val legalEntitiesSaas = listOf(
            SaasValues.legalEntityAugmented1,
            SaasValues.legalEntityAugmented2,
            SaasValues.legalEntityAugmentedNotInPoolResponse,
            SaasValues.legalEntityAugmentedSharingErrorResponse,
            SaasValues.legalEntityAugmentedPendingResponse,
        )

        val legalEntitiesPool = listOf(
            ResponseValues.legalEntityResponsePool1,
            ResponseValues.legalEntityResponsePool2
        )
        val legalAddressesPool = listOf(
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
                                    values = legalEntitiesSaas.map { AugmentedBusinessPartnerResponseSaas(it) }
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
                                    values = legalEntitiesSaas
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_LEGAL_ENTITIES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(legalEntitiesPool)
                        )
                )
        )
        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_LEGAL_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(legalAddressesPool)
                        )
                )
        )

        val paginationValue = PaginationRequest(0, 10)
        val pageResponse = gateClient.legalEntities().getLegalEntitiesOutput(paginationValue, emptyList())
        val expectedResponse = PageOutputResponse(
            total = total,
            nextStartAfter = nextStartAfter,
            content = expectedLegalEntities,
            invalidEntries = expectedPending.size + expectedErrors.size,
            pending = expectedPending,
            errors = expectedErrors,
        )


        testHelpers.assertRecursively(pageResponse).isEqualTo(expectedResponse)
    }

    /**
     * Given legal entities exists in SaaS and bpdm pool
     * When getting legal entities page via output route filtering by external ids
     * Then legal entities page should be returned
     */
    @Test
    fun `get legal entities, filter by external ids`() {
        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutput1,
            ResponseValues.legalEntityGateOutput2
        )

        val legalEntitiesSaas = listOf(
            SaasValues.legalEntityAugmented1,
            SaasValues.legalEntityAugmented2
        )

        val legalEntitiesPool = listOf(
            ResponseValues.legalEntityResponsePool1,
            ResponseValues.legalEntityResponsePool2
        )
        val legalAddressesPool = listOf(
            ResponseValues.logisticAddress1,
            ResponseValues.logisticAddress2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10

        wireMockServerSaas.stubFor(
            get(urlPathMatching(SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalIds", equalTo(listOf(CommonValues.externalId1, CommonValues.externalId2).joinToString(",")))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = limit,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = legalEntitiesSaas.map { AugmentedBusinessPartnerResponseSaas(it) }
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
                                    values = legalEntitiesSaas
                                )
                            )
                        )
                )
        )

        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_LEGAL_ENTITIES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(legalEntitiesPool)
                        )
                )
        )
        wireMockServerBpdmPool.stubFor(
            post(urlPathMatching(POOL_API_MOCK_LEGAL_ADDRESSES_SEARCH_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(legalAddressesPool)
                        )
                )
        )

        val paginationValue = PaginationRequest(0, 10)
        val pageResponse = gateClient.legalEntities().getLegalEntitiesOutput(paginationValue, listOf(CommonValues.externalId1, CommonValues.externalId2))
        val expectedResponse = PageOutputResponse(
            total = total,
            nextStartAfter = nextStartAfter,
            content = expectedLegalEntities,
            invalidEntries = 0,
            pending = listOf(),
            errors = listOf(),
        )
        testHelpers.assertRecursively(pageResponse).isEqualTo(expectedResponse)
    }
}