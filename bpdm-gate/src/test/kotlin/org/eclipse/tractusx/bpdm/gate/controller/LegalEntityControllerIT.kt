package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertRequest
import org.eclipse.tractusx.bpdm.common.dto.cdq.UpsertResponse
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

        val expectedLegalEntities = listOf(
            CdqValues.legalEntity1,
            CdqValues.legalEntity2
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
            .isCreated
            .expectBody(UpsertResponse::class.java)
            .returnResult()
            .responseBody

        val body = wireMockServer.allServeEvents.single().request.bodyAsString
        val upsertRequest = objectMapper.readValue(body, UpsertRequest::class.java)
        assertThat(upsertRequest.businessPartners).containsExactlyInAnyOrderElementsOf(expectedLegalEntities)
    }
}