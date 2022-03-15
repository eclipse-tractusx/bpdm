package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CdqControllerIT(@Autowired val webTestClient: WebTestClient) {

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
    fun importBusinessPartners() {
        wireMockServer.stubFor(
            get(urlPathMatching("/test-cdq-api/storages/test-cdq-storage/businesspartners")).willReturn(
                aResponse().withHeader("Content-Type", "application/json")
                    .withBody(
                        CdqControllerIT::class.java.classLoader.getResource("business-partners.json")!!.readText()
                    )
            )
        )

        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerResponse::class.java).hasSize(2)

        webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody()
            .jsonPath("$.totalElements").isEqualTo(2)
    }
}