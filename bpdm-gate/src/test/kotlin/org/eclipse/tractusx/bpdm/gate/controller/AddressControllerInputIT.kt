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
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.CommonValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_INPUT_ADDRESSES_PATH
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class AddressControllerInputIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val objectMapper: ObjectMapper,
    val cdqConfigProperties: CdqConfigProperties
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
     * Given legal entities and sites in cdq
     * When upserting addresses of legal entities and sites
     * Then upsert addresses and relations in cdq api should be called with the address data mapped to the cdq data model
     */
    @Test
    fun `upsert addresses`() {
        val addresses = listOf(
            RequestValues.addressGateInput1,
            RequestValues.addressGateInput2
        )

        val parentLegalEntitiesCdq = listOf(
            CdqValues.legalEntity1
        )

        val parentSitesCdq = listOf(
            CdqValues.siteBusinessPartner1
        )

        val expectedAddresses = listOf(
            CdqValues.addressBusinessPartner1,
            CdqValues.addressBusinessPartner2
        )

        val expectedRelations = listOf(
            CdqValues.relationAddress1ToLegalEntity,
            CdqValues.relationAddress2ToSite
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceLegalEntity))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 1,
                                    values = parentLegalEntitiesCdq
                                )
                            )
                        )
                )
        )
        // mock "get parent sites"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.siteExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceSite))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 1,
                                    values = parentSitesCdq
                                )
                            )
                        )
                )
        )
        val stubMappingUpsertAddresses = wireMockServer.stubFor(
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

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isOk

        val upsertAddressesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertAddresses, objectMapper).single()
        assertThat(upsertAddressesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedAddresses)

        val upsertRelationsRequest = wireMockServer.deserializeMatchedRequests<UpsertRelationsRequestCdq>(stubMappingUpsertRelations, objectMapper).single()
        assertThat(upsertRelationsRequest.relations).containsExactlyInAnyOrderElementsOf(expectedRelations)
    }

    /**
     * When upserting addresses of legal entities using a legal entity external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert addresses, legal entity parent not found`() {
        val addresses = listOf(
            RequestValues.addressGateInput1
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceLegalEntity))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 0,
                                    values = emptyList<BusinessPartnerCdq>()
                                )
                            )
                        )
                )
        )

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upserting addresses of sites using a site external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert addresses, site parent not found`() {
        val addresses = listOf(
            RequestValues.addressGateInput2
        )

        // mock "get parent sites"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.mapNotNull { it.siteExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceSite))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 0,
                                    values = emptyList<BusinessPartnerCdq>()
                                )
                            )
                        )
                )
        )

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upserting an address without reference to either a parent site or a parent legal entity
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert address without any parent`() {
        val addresses = listOf(
            RequestValues.addressGateInput1.copy(
                siteExternalId = null,
                legalEntityExternalId = null
            )
        )

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upserting an address without reference to both a parent site and a parent legal entity
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert address with site and legal entity parents`() {
        val addresses = listOf(
            RequestValues.addressGateInput1.copy(
                siteExternalId = CommonValues.externalIdSite1,
                legalEntityExternalId = CommonValues.externalId1
            )
        )

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}