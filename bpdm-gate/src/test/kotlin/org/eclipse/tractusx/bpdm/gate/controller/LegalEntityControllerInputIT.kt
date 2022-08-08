package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCollectionCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchResponse
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertRequest
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertResponse
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_INPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
internal class LegalEntityControllerInputIT @Autowired constructor(
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
     * When upserting legal entities
     * Then cdq upsert api should be called with the legal entity data mapped to the cdq data model
     */
    @Test
    fun `upsert legal entities`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInput1,
            RequestValues.legalEntityGateInput2
        )

        val expectedLegalEntities = listOf(
            CdqValues.legalEntity1.copy(dataSource = cdqConfigProperties.datasource),
            CdqValues.legalEntity2.copy(dataSource = cdqConfigProperties.datasource)
        )

        wireMockServer.stubFor(
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

        webTestClient.put().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(legalEntities))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpsertResponse::class.java)
            .returnResult()
            .responseBody

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
            objectMapper.valueToTree<ObjectNode>(RequestValues.legalEntityGateInput1)
                .apply { remove(LegalEntityGateInput::externalId.name) }
        )

        webTestClient.put().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
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
            RequestValues.legalEntityGateInput1,
            LegalEntityGateInput("external-1", RequestValues.legalEntityGateInput1.legalEntity.copy()),
            LegalEntityGateInput("external-2", RequestValues.legalEntityGateInput1.legalEntity.copy())
        )

        webTestClient.put().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(legalEntities))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upserting legal entities with duplicate external ids within the same request
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, duplicate external id`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInput1,
            LegalEntityGateInput(RequestValues.legalEntityGateInput1.externalId, RequestValues.legalEntityGateInput1.legalEntity.copy())
        )

        webTestClient.put().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(legalEntities))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When cdq api responds with an error status code while upserting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `upsert legal entities, cdq error`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInput1,
            RequestValues.legalEntityGateInput2
        )

        wireMockServer.stubFor(
            put(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.put().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(legalEntities))
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given legal entity exists in cdq
     * When getting legal entity by external id
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id`() {
        val expectedLegalEntity = RequestValues.legalEntityGateInput1

        wireMockServer.stubFor(
            post(urlPathMatching(CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                FetchResponse(
                                    businessPartner = CdqValues.legalEntity1,
                                    status = FetchResponse.Status.OK
                                )
                            )
                        )
                )
        )

        val legalEntity = webTestClient.get().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(LegalEntityGateInput::class.java)
            .returnResult()
            .responseBody

        assertThat(legalEntity).usingRecursiveComparison().isEqualTo(expectedLegalEntity)
    }

    /**
     * Given legal entity does not exist in cdq
     * When getting legal entity by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        webTestClient.get().uri("$CATENA_INPUT_LEGAL_ENTITIES_PATH/nonexistent-externalid123")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    /**
     * When cdq api responds with an error status code while fetching legal entity by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entity by external id, cdq error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given legal entity without legal address in CDQ
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get legal entity without legal address, expect error`() {

        val invalidPartner = CdqValues.legalEntity1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        webTestClient.get().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }


    /**
     * Given legal entity exists in cdq
     * When getting legal entities page
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entities`() {
        val legalEntitiesCdq = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2
        )

        val expectedLegalEntities = listOf(
            RequestValues.legalEntityGateInput1,
            RequestValues.legalEntityGateInput2
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
                                BusinessPartnerCollectionCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = legalEntitiesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_LEGAL_ENTITIES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<LegalEntityGateInput>>()
            .responseBody
            .blockFirst()!!

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
     * Given legal entity without legal address in CDQ
     * When getting legal entities page
     * Then only valid legal entities on page returned
     */
    @Test
    fun `filter legal entities without legal address`() {
        val legalEntitiesCdq = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2,
            CdqValues.legalEntity1.copy(addresses = emptyList())
        )

        val expectedLegalEntities = listOf(
            RequestValues.legalEntityGateInput1,
            RequestValues.legalEntityGateInput2
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
                                BusinessPartnerCollectionCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = legalEntitiesCdq
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_INPUT_LEGAL_ENTITIES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<LegalEntityGateInput>>()
            .responseBody
            .blockFirst()!!

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
     * When cdq api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, cdq error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_INPUT_LEGAL_ENTITIES_PATH)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * When cdq api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, pagination limit exceeded`() {
        webTestClient.get().uri { builder ->
            builder.path(CATENA_INPUT_LEGAL_ENTITIES_PATH)
                .queryParam(PaginationStartAfterRequest::limit.name, 999999)
                .build()
        }
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}