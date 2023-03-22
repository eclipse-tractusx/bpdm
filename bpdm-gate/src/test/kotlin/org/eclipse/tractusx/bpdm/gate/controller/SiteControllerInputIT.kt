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
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationStatus
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_DELETE_RELATIONS_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_RELATIONS_PATH
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class SiteControllerInputIT @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val saasConfigProperties: SaasConfigProperties,
    val gateClient: GateClient
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }

    /**
     * Given site exists in SaaS
     * When getting site by external id
     * Then site mapped to the catena data model should be returned
     */
    @Test
    fun `get site by external id`() {
        val expectedSite = ResponseValues.siteGateInputResponse1

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = SaasValues.siteBusinessPartnerWithRelations1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val site = gateClient.sites().getSiteByExternalId(SaasValues.siteBusinessPartnerWithRelations1.externalId.toString())

        assertThat(site).usingRecursiveComparison().isEqualTo(expectedSite)
    }

    /**
     * Given site does not exist in SaaS
     * When getting site by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get site by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        try {
            gateClient.sites().getSiteByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }

    /**
     * When SaaS api responds with an error status code while fetching site by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get site by external id, SaaS error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try {
            gateClient.sites().getSiteByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }
    }

    /**
     * Given site without main address in SaaS
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get site without main address, expect error`() {

        val invalidPartner = SaasValues.siteBusinessPartnerWithRelations1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        try {
            gateClient.sites().getSiteByExternalId(SaasValues.siteBusinessPartnerWithRelations1.externalId.toString())
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }

    }

    /**
     * Given sites exists in SaaS
     * When getting sites page
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites`() {
        val sitesSaas = listOf(
            SaasValues.siteBusinessPartnerWithRelations1,
            SaasValues.siteBusinessPartnerWithRelations2
        )

        val expectedSites = listOf(
            ResponseValues.siteGateInputResponse1,
            ResponseValues.siteGateInputResponse2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 0

        wireMockServer.stubFor(
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

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.sites().getSites(paginationValue)

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
     * Given invalid sites in SaaS
     * When getting sites page
     * Then only valid sites on page returned
     */
    @Test
    fun `filter invalid sites`() {
        val sitesSaas = listOf(
            SaasValues.siteBusinessPartnerWithRelations1,
            SaasValues.siteBusinessPartnerWithRelations2,
            SaasValues.siteBusinessPartnerWithRelations1.copy(addresses = emptyList()), // site without address
            SaasValues.siteBusinessPartnerWithRelations1.copy(names = listOf()), // site without names
            SaasValues.siteBusinessPartnerWithRelations1.copy(relations = listOf()) // site without legal entity parent
        )

        val expectedSites = listOf(
            ResponseValues.siteGateInputResponse1,
            ResponseValues.siteGateInputResponse2
        )

        val limit = 5
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 3

        wireMockServer.stubFor(
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

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.sites().getSites(paginationValue)

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
     * When SaaS api responds with an error status code while getting sites
     * Then an internal server error response should be sent
     */
    @Test
    fun `get sites, SaaS error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        val paginationValue = PaginationStartAfterRequest("", 10)

        try {
            gateClient.sites().getSites(paginationValue)
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }

    }

    /**
     * When requesting too many sites
     * Then a bad request response should be sent
     */
    @Test
    fun `get sites, pagination limit exceeded`() {

        val paginationValue = PaginationStartAfterRequest("", 999999)

        try {
            gateClient.sites().getSites(paginationValue)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entities in SaaS
     * When upserting sites of legal entities
     * Then upsert sites and relations in SaaS api should be called with the site data mapped to the SaaS data model
     */
    @Test
    fun `upsert sites`() {
        val sites = listOf(
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )

        val parentLegalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.legalEntityResponse2
        )

        val expectedSites = listOf(
            SaasValues.siteBusinessPartnerRequest1,
            SaasValues.siteBusinessPartnerRequest2
        )

        val expectedRelations = listOf(
            SaasValues.relationSite1ToLegalEntity,
            SaasValues.relationSite2ToLegalEntity
        )

        val expectedDeletedRelations = listOf(
            DeleteRelationsRequestSaas.RelationToDeleteSaas(
                startNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.relationSite1ToLegalEntity.startNode
                ),
                endNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.relationSite1ToLegalEntity.endNode
                ),
            ),
            DeleteRelationsRequestSaas.RelationToDeleteSaas(
                startNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.relationSite2ToLegalEntity.startNode
                ),
                endNode = DeleteRelationsRequestSaas.RelationNodeToDeleteSaas(
                    dataSourceId = saasConfigProperties.datasource,
                    externalId = SaasValues.relationSite2ToLegalEntity.endNode
                ),
            ),
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 2,
                                    values = parentLegalEntitiesSaas
                                )
                            )
                        )
                )
        )
        val stubMappingUpsertSites = wireMockServer.stubFor(
            put(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
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
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.externalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .withQueryParam("featuresOn", containing("FETCH_RELATIONS"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 2,
                                    values = listOf(
                                        SaasValues.siteBusinessPartnerWithRelations1,
                                        SaasValues.siteBusinessPartnerWithRelations2
                                    )
                                )
                            )
                        )
                )
        )

        val stubMappingDeleteRelations = wireMockServer.stubFor(
            post(urlPathMatching(SAAS_MOCK_DELETE_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                DeleteRelationsResponseSaas(2)
                            )
                        )
                )
        )

        val stubMappingUpsertRelations = wireMockServer.stubFor(
            put(urlPathMatching(SAAS_MOCK_RELATIONS_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                UpsertRelationsResponseSaas(
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

        try {
            gateClient.sites().upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        // check that "upsert sites" was called in SaaS as expected
        val upsertSitesRequest = wireMockServer.deserializeMatchedRequests<UpsertRequest>(stubMappingUpsertSites, objectMapper).single()
        assertThat(upsertSitesRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedSites)

        // check that "delete relations" was called in SaaS as expected
        val deleteRelationsRequestSaas =
            wireMockServer.deserializeMatchedRequests<DeleteRelationsRequestSaas>(stubMappingDeleteRelations, objectMapper).single()
        assertThat(deleteRelationsRequestSaas.relations).containsExactlyInAnyOrderElementsOf(expectedDeletedRelations)

        // check that "upsert relations" was called in SaaS as expected
        val upsertRelationsRequest = wireMockServer.deserializeMatchedRequests<UpsertRelationsRequestSaas>(stubMappingUpsertRelations, objectMapper).single()
        assertThat(upsertRelationsRequest.relations).containsExactlyInAnyOrderElementsOf(expectedRelations)
    }

    /**
     * Given legal entities in SaaS
     * When upserting sites of legal entities using a legal entity external id that does not exist
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert sites, legal entity parent not found`() {
        val sites = listOf(
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )
        val parentLegalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1
        )

        // mock "get parent legal entities"
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .withQueryParam("externalId", equalTo(sites.map { it.legalEntityExternalId }.joinToString(",")))
                .withQueryParam("dataSource", equalTo(saasConfigProperties.datasource))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseSaas(
                                    limit = 50,
                                    total = 1,
                                    values = parentLegalEntitiesSaas
                                )
                            )
                        )
                )
        )

        try {
            gateClient.sites().upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given valid site
     * When validate that site
     * Then response is OK and no errors
     */
    @Test
    fun `validate a valid site`() {
        val site = RequestValues.siteGateInputRequest1

        val mockParent = SaasValues.legalEntityResponse1
        val mockParentResponse = PagedResponseSaas(1, null, null, 1, listOf(mockParent))
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockParentResponse))
                )
        )

        val mockDefects = listOf(
            DataDefectSaas(ViolationLevel.INFO, "Info"),
            DataDefectSaas(ViolationLevel.NO_DEFECT, "No Defect"),
            DataDefectSaas(ViolationLevel.WARNING, "Warning"),
        )
        val mockResponse = ValidationResponseSaas(mockDefects)
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_DATA_VALIDATION_BUSINESSPARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))
                )
        )

        val actualResponse = gateClient.sites().validateSite(site)

        val expectedResponse = ValidationResponse(ValidationStatus.OK, emptyList())

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    /**
     * Given invalid site
     * When validate that site
     * Then response is ERROR and contain error description
     */
    @Test
    fun `validate an invalid site`() {
        val site = RequestValues.siteGateInputRequest1


        val mockParent = SaasValues.legalEntityResponse1
        val mockParentResponse = PagedResponseSaas(1, null, null, 1, listOf(mockParent))
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockParentResponse))
                )
        )


        val mockErrorMessage = "Validation error"
        val mockDefects = listOf(
            DataDefectSaas(ViolationLevel.ERROR, mockErrorMessage),
            DataDefectSaas(ViolationLevel.INFO, "Info"),
            DataDefectSaas(ViolationLevel.NO_DEFECT, "No Defect"),
            DataDefectSaas(ViolationLevel.WARNING, "Warning"),
        )

        val mockResponse = ValidationResponseSaas(mockDefects)
        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.SAAS_MOCK_DATA_VALIDATION_BUSINESSPARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockResponse))
                )
        )

        val actualResponse = gateClient.sites().validateSite(site)

        val expectedResponse = ValidationResponse(ValidationStatus.ERROR, listOf(mockErrorMessage))

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }
}