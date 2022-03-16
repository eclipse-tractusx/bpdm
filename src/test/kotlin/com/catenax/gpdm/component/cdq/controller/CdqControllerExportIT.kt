package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CdqControllerExportIT(@Autowired val webTestClient: WebTestClient) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @Test
    @DirtiesContext
    fun `import, then export business partners`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners.json"))
                )
        )

        wireMockServer.stubFor(
            put(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                               "numberOfAccepted": 2,
                               "numberOfFailed": 0,
                               "featuresOn": [],
                               "failures": []
                            }
                        """.trimIndent()
                        )
                )
        )

        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerResponse::class.java).hasSize(2)

        webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody()
            .jsonPath("$.totalElements").isEqualTo(2)

        webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerCdq::class.java).hasSize(2)
    }

    private fun readTestResource(testResourcePath: String) =
        CdqControllerExportIT::class.java.classLoader.getResource(testResourcePath)!!.readText()
}