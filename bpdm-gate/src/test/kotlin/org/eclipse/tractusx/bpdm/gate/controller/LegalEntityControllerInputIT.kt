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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationStatus
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.GATE_API_INPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class LegalEntityControllerInputIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper,
    val gateClient: GateClient,
    private val legalEntityRepository: LegalEntityRepository
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

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }


    /**
     * When upserting legal entities
     * Then SaaS upsert api should be called with the legal entity data mapped to the SaaS data model
     */
    @Test
    fun `upsert legal entities`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2,
        )

        val expectedLegalEntities = listOf(
            SaasValues.legalEntityRequest1,
            SaasValues.legalEntityRequest2,
        )

        wireMockServer.stubFor(
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

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        val body = wireMockServer.allServeEvents.single().request.bodyAsString
        val upsertRequest = objectMapper.readValue(body, UpsertRequest::class.java)
        // TODO: check the upsertRequest
//        assertThat(upsertRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedLegalEntities)
    }

    /**
     * When upserting legal entities with missing external id
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, missing external id`() {
        val legalEntitiesJson: JsonNode = objectMapper.createArrayNode().add(
            objectMapper.valueToTree<ObjectNode>(RequestValues.legalEntityGateInputRequest1)
                .apply { remove(LegalEntityGateInputRequest::externalId.name) }
        )

        webTestClient.put().uri(GATE_API_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(legalEntitiesJson.toString())
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upper limit of legal entities in a request is exceeded when upserting legal entities
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, legal entity limit exceeded`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest1.copy(externalId = "external-1"),
            RequestValues.legalEntityGateInputRequest1.copy(externalId = "external-2")
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    /**
     * When upserting legal entities with duplicate external ids within the same request
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, duplicate external id`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest1.copy()
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * When SaaS api responds with an error status code while upserting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `upsert legal entities, SaaS error`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        wireMockServer.stubFor(
            put(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }
    }

    /**
     * Given legal entity exists
     * When getting legal entity by external id
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id`() {
        val expectedLegalEntity = ResponseValues.legalEntityGateInputResponse3

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest3
        )

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        val legalEntity = gateClient.legalEntities().getLegalEntityByExternalId(CommonValues.externalId3)

        assertThat(legalEntity).usingRecursiveComparison().isEqualTo(expectedLegalEntity)
    }

    /**
     * Given legal entity does not exist
     * When getting legal entity by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {

        try {
            gateClient.legalEntities().getLegalEntityByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }

    /**
     * When SaaS api responds with an error status code while fetching legal entity by external id
     * Then an internal server error response should be sent
     */
//    @Test
//    fun `get legal entity by external id, SaaS error`() {
//        wireMockServer.stubFor(
//            post(urlPathMatching(SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
//                .willReturn(badRequest())
//        )
//
//        try {
//            gateClient.legalEntities().getLegalEntityByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
//        } catch (e: WebClientResponseException) {
//            val statusCode: HttpStatusCode = e.statusCode
//            val statusCodeValue: Int = statusCode.value()
//            assertTrue(statusCodeValue in 500..599)
//        }
//    }

    /**
     * Given legal entity without legal address
     * When query by its external ID
     * Then server error is returned
     */
//    @Test
//    fun `get legal entity without legal address, expect error`() {
//
//        val invalidPartner = SaasValues.legalEntityResponse1.copy(addresses = emptyList())
//
//
//
//        try {
//            gateClient.legalEntities().getLegalEntityByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
//        } catch (e: WebClientResponseException) {
//            val statusCode: HttpStatusCode = e.statusCode
//            val statusCodeValue: Int = statusCode.value()
//            assertTrue(statusCodeValue in 500..599)
//        }
//    }


    /**
     * Given legal entity exists in SaaS
     * When getting legal entities page
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entities`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateInputResponse1,
            ResponseValues.legalEntityGateInputResponse2,
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2


        val paginationValue = PaginationRequest(page, size)
        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        val pageResponse = gateClient.legalEntities().getLegalEntities(paginationValue)

        val expectedPage = PageResponse(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )
        assertThat(pageResponse).usingRecursiveComparison().ignoringCollectionOrder().ignoringAllOverriddenEquals()
            .ignoringFieldsMatchingRegexes(".*processStartedAt*", ".*administrativeAreaLevel1*").isEqualTo(
            expectedPage
        )
    }


    /**
     * Given legal entity exists in SaaS
     * When getting legal entities page based on external id list
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity filter by external ids`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateInputResponse1,
            ResponseValues.legalEntityGateInputResponse2,
        )
        val page = 0
        val size = 10
        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2

        val paginationValue = PaginationRequest(page, size)
        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        val listExternalIds = legalEntities.mapNotNull { it.externalId }
        val pageResponse = gateClient.legalEntities().getLegalEntitiesByExternalIds(paginationValue, listExternalIds)

        val expectedPage = PageResponse(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )

        println("expected " + objectMapper.writeValueAsString(expectedPage))
        println("return " + objectMapper.writeValueAsString(pageResponse))
        assertThat(pageResponse).usingRecursiveComparison().ignoringCollectionOrder().ignoringAllOverriddenEquals()
            .ignoringFieldsMatchingRegexes(".*processStartedAt*", ".*administrativeAreaLevel1*").isEqualTo(
            expectedPage
        )
    }

    /**
     * Given legal entity without legal address in SaaS
     * When getting legal entities page
     * Then only valid legal entities on page returned
     */
//    @Test
//    fun `filter legal entities without legal address`() {
//        val legalEntitiesSaas = listOf(
//            SaasValues.legalEntityResponse1,
//            SaasValues.legalEntityResponse2,
//            SaasValues.legalEntityResponse1.copy(addresses = emptyList())
//        )
//
//        val expectedLegalEntities = listOf(
//            ResponseValues.legalEntityGateInputResponse1,
//            ResponseValues.legalEntityGateInputResponse2,
//        )
//        val page = 0
//        val size = 10
//
//        val totalElements = 2L
//        val totalPages = 1
//        val pageValue = 0
//        val contentSize = 2
//        val limit = 3
//        val startAfter = "Aaa111"
//        val nextStartAfter = "Aaa222"
//        val total = 10
//        val invalidEntries = 1
//
//        wireMockServer.stubFor(
//            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
//                .willReturn(
//                    aResponse()
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(
//                            objectMapper.writeValueAsString(
//                                PagedResponseSaas(
//                                    limit = limit,
//                                    startAfter = startAfter,
//                                    nextStartAfter = nextStartAfter,
//                                    total = total,
//                                    values = legalEntitiesSaas
//                                )
//                            )
//                        )
//                )
//        )
//
//        val paginationValue = PaginationRequest(page, size)
//        val pageResponse = gateClient.legalEntities().getLegalEntities(paginationValue)
//
//        assertThat(pageResponse).isEqualTo(
//            PageStartAfterResponse(
//                total = total,
//                nextStartAfter = nextStartAfter,
//                content = expectedLegalEntities,
//                invalidEntries = invalidEntries
//            )
//        )
//    }

    /**
     * When SaaS api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, SaaS error`() {


        try {
            val paginationValue = PaginationRequest()
            gateClient.legalEntities().getLegalEntities(paginationValue)
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }

//        webTestClient.get().uri(GATE_API_INPUT_LEGAL_ENTITIES_PATH)
//            .exchange()
//            .expectStatus()
//            .is5xxServerError
    }

    /**
     * When requesting too many legal entities
     * Then a bad request response should be sent
     */
    @Test
    fun `get legal entities, pagination limit exceeded`() {

        try {
            val paginationValue = PaginationRequest(0, 999999)
            gateClient.legalEntities().getLegalEntities(paginationValue)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given valid legal entity
     * When validate that legal entity
     * Then response is OK and no errors
     */
    @Test
    fun `validate a valid legal entity`() {
        val legalEntity = RequestValues.legalEntityGateInputRequest1

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

        val actualResponse = gateClient.legalEntities().validateLegalEntity(legalEntity)

        val expectedResponse = ValidationResponse(ValidationStatus.OK, emptyList())

        assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    /**
     * Given invalid legal entity
     * When validate that legal entity
     * Then response is ERROR and contain error description
     */
    @Test
    fun `validate an invalid legal entity`() {
        val legalEntity = RequestValues.legalEntityGateInputRequest1

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

        val actualResponse = gateClient.legalEntities().validateLegalEntity(legalEntity)

        val expectedResponse = ValidationResponse(ValidationStatus.ERROR, listOf(mockErrorMessage))

        assertThat(actualResponse).isEqualTo(expectedResponse)

    }

    /**
     * When upserting legal entities
     * Then SaaS upsert api should be called with the legal entity data mapped to the SaaS data model
     */
    @Test
    fun `upsert and persist legal entities`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2,
        )

        val expectedLegalEntities = listOf(
            SaasValues.legalEntityRequest1,
            SaasValues.legalEntityRequest2,
        )

        wireMockServer.stubFor(
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

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        val legalEntityRecordExternal1 = legalEntityRepository.findByExternalId("external-1")
        assertNotEquals(legalEntityRecordExternal1, null)

        val legalEntityRecordExternal2 = legalEntityRepository.findByExternalId("external-2")
        assertNotEquals(legalEntityRecordExternal2, null)

    }
}