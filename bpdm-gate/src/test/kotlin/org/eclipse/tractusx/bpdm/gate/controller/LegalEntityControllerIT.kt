package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertResponse
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
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

private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class LegalEntityControllerIT @Autowired constructor(
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

    @Test
    fun `upsert legal entities`() {
        val legalEntities = listOf(
            RequestValues.legalEntity1,
            RequestValues.legalEntity2
        )

        wireMockServer.stubFor(
            WireMock.put(WireMock.urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    WireMock.aResponse()
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
            .isCreated
            .expectBody(UpsertResponse::class.java)
            .returnResult()
            .responseBody
    }
}