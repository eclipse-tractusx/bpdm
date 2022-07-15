package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchResponse
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertRequest
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertResponse
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
internal class LegalEntityControllerIT @Autowired constructor(
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
            RequestValues.legalEntity1,
            RequestValues.legalEntity2
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

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
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
            objectMapper.valueToTree<ObjectNode>(RequestValues.legalEntity1)
                .apply { remove(LegalEntityDto::externalId.name) }
        )

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
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
            RequestValues.legalEntity1,
            RequestValues.legalEntity1.copy("external-2"),
            RequestValues.legalEntity1.copy("external-3")
        )

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
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
            RequestValues.legalEntity1,
            RequestValues.legalEntity1.copy()
        )

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
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
            RequestValues.legalEntity1,
            RequestValues.legalEntity2
        )

        wireMockServer.stubFor(
            put(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
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
        val expectedLegalEntity = RequestValues.legalEntity1

        wireMockServer.stubFor(
            post(urlPathMatching(EndpointValues.CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH))
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

        val legalEntity = webTestClient.get().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1.externalId}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(LegalEntityDto::class.java)
            .returnResult()
            .responseBody

        assertThat(legalEntity).isEqualTo(expectedLegalEntity)
    }

    /**
     * Given legal entity does not exist in cdq
     * When getting legal entity by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {
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

        webTestClient.get().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1.externalId}")
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
        val legalEntities = listOf(
            RequestValues.legalEntity1,
            RequestValues.legalEntity2
        )

        wireMockServer.stubFor(
            put(urlPathMatching(CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.put().uri(EndpointValues.CATENA_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(legalEntities))
            .exchange()
            .expectStatus()
            .is5xxServerError
    }
}