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

package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_INPUT_SITES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_RELATIONS_PATH
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
import org.eclipse.tractusx.bpdm.gate.util.deserializeMatchedRequests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
internal class SiteControllerInputIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val objectMapper: ObjectMapper
) {
    companion object {
        @RegisterExtension
        val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    /**
     * Given legal entities in cdq
     * When upserting sites of legal entities
     * Then upsert sites and relations in cdq api should be called with the site data mapped to the cdq data model
     */
    @Test
    fun `upsert sites`() {
        val sites = listOf(
            RequestValues.siteGateInput1,
            RequestValues.siteGateInput2
        )

        val legalEntitiesCdq = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2
        )

        val expectedSites = listOf(
            CdqValues.site1,
            CdqValues.site2
        )

        val expectedRelations = listOf(
            CdqValues.relationSite1ToLegalEntity,
            CdqValues.relationSite2ToLegalEntity
        )

        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 2,
                                    total = 2,
                                    values = legalEntitiesCdq
                                )
                            )
                        )
                )
        )
        val stubMappingUpsertSites = wireMockServer.stubFor(
            put(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                UpsertResponse(
                                    emptyList(),
                                    emptyList(),
                                    2,
                                    0
                                )
                            )
                        )
                )
        )
        val stubMappingUpsertRelations = wireMockServer.stubFor(
            put(urlPathMatching(CDQ_MOCK_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                UpsertRelationsResponseCdq(
                                    failures = emptyList(),
                                    numberOfFailed = 0,
                                    numberOfInserts = 2,
                                    numberOfProvidedRelations = 2,
                                    numberOfUpdates = 0
                                )
                            )
                        )
                )
        )

        webTestClient.put().uri(CATENA_INPUT_SITES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(sites))
            .exchange()
            .expectStatus()
            .isOk

        val upsertSitesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertSites, objectMapper).single()
        Assertions.assertThat(upsertSitesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedSites)

        val upsertRelationsRequest = wireMockServer.deserializeMatchedRequests<Collection<RelationCdq>>(stubMappingUpsertRelations, objectMapper).single()
        Assertions.assertThat(upsertRelationsRequest).containsExactlyInAnyOrderElementsOf(expectedRelations)
    }
}