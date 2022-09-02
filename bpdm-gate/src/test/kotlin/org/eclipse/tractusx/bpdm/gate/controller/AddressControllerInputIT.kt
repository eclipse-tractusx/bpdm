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
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_INPUT_ADDRESSES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_RELATIONS_PATH
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
internal class AddressControllerInputIT @Autowired constructor(
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
     * Given address exists in cdq
     * When getting address by external id
     * Then address mapped to the catena data model should be returned
     */
    @Test
    fun `get address by external id`() {
        val expectedAddress = RequestValues.addressGateInput1

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = CdqValues.addressBusinessPartnerWithRelations1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val address = webTestClient.get().uri(CATENA_INPUT_ADDRESSES_PATH + "/${CdqValues.addressBusinessPartnerWithRelations1.externalId}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(AddressGateInput::class.java)
            .returnResult()
            .responseBody

        assertThat(address).usingRecursiveComparison().isEqualTo(expectedAddress)
    }

    /**
     * Given address does not exist in cdq
     * When getting address by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get address by external id, not found`() {
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

        webTestClient.get().uri("${CATENA_INPUT_ADDRESSES_PATH}/nonexistent-externalid123")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    /**
     * When cdq api responds with an error status code while fetching address by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get address by external id, cdq error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_ADDRESSES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given address business partner without address data in CDQ
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get address for which cdq data is invalid, expect error`() {

        val invalidPartner = CdqValues.addressBusinessPartnerWithRelations1.copy(addresses = emptyList())

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

        webTestClient.get().uri(CATENA_INPUT_ADDRESSES_PATH + "/${CdqValues.addressBusinessPartnerWithRelations1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given addresses exists in cdq
     * When getting addresses page
     * Then addresses page mapped to the catena data model should be returned
     */
    @Test
    fun `get addresses`() {
        val addressesCdq = listOf(
            CdqValues.addressBusinessPartnerWithRelations1,
            CdqValues.addressBusinessPartnerWithRelations2
        )

        val expectedAddresses = listOf(
            RequestValues.addressGateInput1,
            RequestValues.addressGateInput2
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
                                    values = addressesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_ADDRESSES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<AddressGateInput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * Given invalid addresses in CDQ
     * When getting addresses page
     * Then only valid addresses on page returned
     */
    @Test
    fun `filter invalid addresses`() {
        val addressesCdq = listOf(
            CdqValues.addressBusinessPartnerWithRelations1,
            CdqValues.addressBusinessPartnerWithRelations2,
            CdqValues.addressBusinessPartnerWithRelations1.copy(addresses = emptyList()), // address without address data
        )

        val expectedAddresses = listOf(
            RequestValues.addressGateInput1,
            RequestValues.addressGateInput2
        )

        val limit = 3
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 1

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
                                    values = addressesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_ADDRESSES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<AddressGateInput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedAddresses,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * When cdq api responds with an error status code while getting addresses
     * Then an internal server error response should be sent
     */
    @Test
    fun `get addresses, cdq error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_ADDRESSES_PATH)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * When requesting too many addresses
     * Then a bad request response should be sent
     */
    @Test
    fun `get addresses, pagination limit exceeded`() {
        webTestClient.get().uri { builder ->
            builder.path(CATENA_INPUT_ADDRESSES_PATH)
                .queryParam(PaginationStartAfterRequest::limit.name, 999999)
                .build()
        }
            .exchange()
            .expectStatus()
            .isBadRequest
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

        val expectedDeletedRelations = listOf(
            DeleteRelationsRequestCdq.RelationToDeleteCdq(
                startNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceLegalEntity,
                    externalId = CdqValues.addressBusinessPartnerWithRelations1.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceAddress,
                    externalId = CdqValues.addressBusinessPartnerWithRelations1.relations.first().endNode
                ),
            ),
            DeleteRelationsRequestCdq.RelationToDeleteCdq(
                startNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceSite,
                    externalId = CdqValues.addressBusinessPartnerWithRelations2.relations.first().startNode
                ),
                endNode = DeleteRelationsRequestCdq.RelationNodeToDeleteCdq(
                    dataSourceId = cdqConfigProperties.datasourceAddress,
                    externalId = CdqValues.addressBusinessPartnerWithRelations2.relations.first().endNode
                ),
            ),
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
        // mock "get addresses with relations"
        // this simulates the case that the address already had some relations
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(addresses.map { it.externalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(cdqConfigProperties.datasourceAddress))
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
                                        CdqValues.addressBusinessPartnerWithRelations1,
                                        CdqValues.addressBusinessPartnerWithRelations2
                                    )
                                )
                            )
                        )
                )
        )
        val stubMappingDeleteRelations = wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_DELETE_RELATIONS_PATH))
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

        webTestClient.put().uri(CATENA_INPUT_ADDRESSES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(addresses))
            .exchange()
            .expectStatus()
            .isOk

        val upsertAddressesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertAddresses, objectMapper).single()
        assertThat(upsertAddressesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedAddresses)

        // check that "delete relations" was called in cdq as expected
        val deleteRelationsRequestCdq = wireMockServer.deserializeMatchedRequests<DeleteRelationsRequestCdq>(stubMappingDeleteRelations, objectMapper).single()
        assertThat(deleteRelationsRequestCdq.relations).containsExactlyInAnyOrderElementsOf(expectedDeletedRelations)

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