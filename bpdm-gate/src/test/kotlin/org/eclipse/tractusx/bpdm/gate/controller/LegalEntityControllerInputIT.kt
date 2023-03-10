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
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.client.config.GateClient
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationStatus
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.GATE_API_INPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
import org.eclipse.tractusx.bpdm.gate.util.ResponseValues
import org.eclipse.tractusx.bpdm.gate.util.SaasValues
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
internal class LegalEntityControllerInputIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper,
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

        try{
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        }catch (e: WebClientResponseException){
            assertEquals(HttpStatus.OK,e.statusCode)
        }

        val body = wireMockServer.allServeEvents.single().request.bodyAsString
        val upsertRequest = objectMapper.readValue(body, UpsertRequest::class.java)
        assertThat(upsertRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedLegalEntities)
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

        try{
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException){
            assertEquals(HttpStatus.BAD_REQUEST,e.statusCode)
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

        try{
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException){
            assertEquals(HttpStatus.BAD_REQUEST,e.statusCode)
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

        try{
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException){
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }
    }

    /**
     * Given legal entity exists in SaaS
     * When getting legal entity by external id
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id`() {
        val expectedLegalEntity = ResponseValues.legalEntityGateInputResponse1

        wireMockServer.stubFor(
            post(urlPathMatching(SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = SaasValues.legalEntityResponse1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val legalEntity = gateClient.legalEntities().getLegalEntityByExternalId(SaasValues.legalEntityRequest1.externalId.toString())

        assertThat(legalEntity).usingRecursiveComparison().isEqualTo(expectedLegalEntity)
    }

    /**
     * Given legal entity does not exist in SaaS
     * When getting legal entity by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        try{
            gateClient.legalEntities().getLegalEntityByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException){
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }

    /**
     * When SaaS api responds with an error status code while fetching legal entity by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entity by external id, SaaS error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try{
            gateClient.legalEntities().getLegalEntityByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
        } catch (e: WebClientResponseException){
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }
    }

    /**
     * Given legal entity without legal address in SaaS
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get legal entity without legal address, expect error`() {

        val invalidPartner = SaasValues.legalEntityResponse1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        try{
            gateClient.legalEntities().getLegalEntityByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
        } catch (e: WebClientResponseException){
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }
    }


    /**
     * Given legal entity exists in SaaS
     * When getting legal entities page
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entities`() {
        val legalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.legalEntityResponse2,
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateInputResponse1,
            ResponseValues.legalEntityGateInputResponse2,
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
                                    values = legalEntitiesSaas
                                )
                            )
                        )
                )
        )

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.legalEntities().getLegalEntities(paginationValue)

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedLegalEntities,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * Given legal entity without legal address in SaaS
     * When getting legal entities page
     * Then only valid legal entities on page returned
     */
    @Test
    fun `filter legal entities without legal address`() {
        val legalEntitiesSaas = listOf(
            SaasValues.legalEntityResponse1,
            SaasValues.legalEntityResponse2,
            SaasValues.legalEntityResponse1.copy(addresses = emptyList())
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateInputResponse1,
            ResponseValues.legalEntityGateInputResponse2,
        )

        val limit = 3
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 1

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
                                    values = legalEntitiesSaas
                                )
                            )
                        )
                )
        )

        val paginationValue = PaginationStartAfterRequest(startAfter, limit)
        val pageResponse = gateClient.legalEntities().getLegalEntities(paginationValue)

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedLegalEntities,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * When SaaS api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, SaaS error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        try{
            val paginationValue = PaginationStartAfterRequest("")
            gateClient.legalEntities().getLegalEntities(paginationValue)
        } catch (e: WebClientResponseException){
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

        try{
            val paginationValue = PaginationStartAfterRequest("", 999999)
            gateClient.legalEntities().getLegalEntities(paginationValue)
        } catch (e: WebClientResponseException){
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
}