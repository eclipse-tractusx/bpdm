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
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_INPUT_SITES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_DELETE_RELATIONS_PATH
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
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class SiteControllerInputIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper,
    private val cdqConfigProperties: CdqConfigProperties
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    /**
     * Given site exists in cdq
     * When getting site by external id
     * Then site mapped to the catena data model should be returned
     */
    @Test
    fun `get site by external id`() {
        val expectedSite = RequestValues.siteGateInput1

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = CdqValues.siteBusinessPartnerWithRelations1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val site = webTestClient.get().uri(CATENA_INPUT_SITES_PATH + "/${CdqValues.siteBusinessPartnerWithRelations1.externalId}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(SiteGateInput::class.java)
            .returnResult()
            .responseBody

        assertThat(site).usingRecursiveComparison().isEqualTo(expectedSite)
    }

    /**
     * Given site does not exist in cdq
     * When getting site by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get site by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = null,
                                    status = FetchResponse.Status.NOT_FOUND
                                )
                            )
                        )
                )
        )

        webTestClient.get().uri("${CATENA_INPUT_SITES_PATH}/nonexistent-externalid123")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    /**
     * When cdq api responds with an error status code while fetching site by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get site by external id, cdq error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_SITES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given site without main address in CDQ
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get site without main address, expect error`() {

        val invalidPartner = CdqValues.siteBusinessPartnerWithRelations1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = invalidPartner,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        webTestClient.get().uri(CATENA_INPUT_SITES_PATH + "/${CdqValues.siteBusinessPartnerWithRelations1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given sites exists in cdq
     * When getting sites page
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites`() {
        val sitesCdq = listOf(
            CdqValues.siteBusinessPartnerWithRelations1,
            CdqValues.siteBusinessPartnerWithRelations2
        )

        val expectedSites = listOf(
            RequestValues.siteGateInput1,
            RequestValues.siteGateInput2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 0

        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_SITES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<SiteGateInput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedSites,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * Given invalid sites in CDQ
     * When getting sites page
     * Then only valid sites on page returned
     */
    @Test
    fun `filter invalid sites`() {
        val sitesCdq = listOf(
            CdqValues.siteBusinessPartnerWithRelations1,
            CdqValues.siteBusinessPartnerWithRelations2,
            CdqValues.siteBusinessPartnerWithRelations1.copy(addresses = emptyList()), // site without address
            CdqValues.siteBusinessPartnerWithRelations1.copy(names = listOf()), // site without names
            CdqValues.siteBusinessPartnerWithRelations1.copy(relations = listOf()) // site without legal entity parent
        )

        val expectedSites = listOf(
            RequestValues.siteGateInput1,
            RequestValues.siteGateInput2
        )

        val limit = 5
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 3

        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = sitesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_SITES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<SiteGateInput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedSites,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * When cdq api responds with an error status code while getting sites
     * Then an internal server error response should be sent
     */
    @Test
    fun `get sites, cdq error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_SITES_PATH)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * When requesting too many sites
     * Then a bad request response should be sent
     */
    @Test
    fun `get sites, pagination limit exceeded`() {
        webTestClient.get().uri { builder ->
            builder.path(CATENA_INPUT_SITES_PATH)
                .queryParam(PaginationStartAfterRequest::limit.name, 999999)
                .build()
        }
            .exchange()
            .expectStatus()
            .isBadRequest
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

        val parentLegalEntitiesCdq = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2
        )

        val expectedSites = listOf(
            CdqValues.siteBusinessPartner1,
            CdqValues.siteBusinessPartner2
        )

        val expectedRelations = listOf(
            CdqValues.relationSite1ToLegalEntity,
            CdqValues.relationSite2ToLegalEntity
        )

        val expectedDeletedRelations = listOf(
            DeleteRelationsRequestCdq.RelationToDeleteCdq(
                startNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceLegalEntity,
                    externalId = CdqValues.siteBusinessPartnerWithRelations1.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceSite,
                    externalId = CdqValues.siteBusinessPartnerWithRelations1.relations.first().endNode
                ),
            ),
            DeleteRelationsRequestCdq.RelationToDeleteCdq(
                startNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceLegalEntity,
                    externalId = CdqValues.siteBusinessPartnerWithRelations2.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceSite,
                    externalId = CdqValues.siteBusinessPartnerWithRelations2.relations.first().endNode
                ),
            ),
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceLegalEntity))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 2,
                                    values = parentLegalEntitiesCdq
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
        // mock "get sites with relations"
        // this simulates the case that the site already had some relations
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.externalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceSite))
                .withQueryParam("featuresOn", containing("FETCH_RELATIONS"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = 50,
                                    total = 2,
                                    values = listOf(
                                        CdqValues.siteBusinessPartnerWithRelations1,
                                        CdqValues.siteBusinessPartnerWithRelations2
                                    )
                                )
                            )
                        )
                )
        )

        val stubMappingDeleteRelations = wireMockServer.stubFor(
            post(urlPathMatching(CDQ_MOCK_DELETE_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                DeleteRelationsResponseCdq(2)
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

        // check that "upsert sites" was called in cdq as expected
        val upsertSitesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertSites, objectMapper).single()
        assertThat(upsertSitesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedSites)

        // check that "delete relations" was called in cdq as expected
        val deleteRelationsRequestCdq = wireMockServer.deserializeMatchedRequests<DeleteRelationsRequestCdq>(stubMappingDeleteRelations, objectMapper).single()
        assertThat(deleteRelationsRequestCdq.relations).containsExactlyInAnyOrderElementsOf(expectedDeletedRelations)

        // check that "upsert relations" was called in cdq as expected
        val upsertRelationsRequest = wireMockServer.deserializeMatchedRequests<UpsertRelationsRequestCdq>(stubMappingUpsertRelations, objectMapper).single()
        assertThat(upsertRelationsRequest.relations).containsExactlyInAnyOrderElementsOf(expectedRelations)
    }

    /**
     * Given legal entities in cdq
     * When upserting sites of legal entities using a legal entity external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert sites, legal entity parent not found`() {
        val sites = listOf(
            RequestValues.siteGateInput1,
            RequestValues.siteGateInput2
        )
        val parentLegalEntitiesCdq = listOf(
            CdqValues.legalEntity1
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.legalEntityExternalId }.joinToString(",")))
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

        webTestClient.put().uri(CATENA_INPUT_SITES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(sites))
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}