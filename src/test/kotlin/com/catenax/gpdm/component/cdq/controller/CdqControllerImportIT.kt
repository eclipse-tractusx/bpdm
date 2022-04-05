package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.ImportResponse
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CdqControllerImportIT @Autowired constructor(val webTestClient: WebTestClient, val cdqIdProperties: CdqIdentifierConfigProperties) {

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
    fun `given new partners in cdq, when import partners from cdq, then partners imported`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(readTestResource("cdq/business-partners.json"))
            )
        )

        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(ImportResponse::class.java).returnResult().responseBody

        val savedBusinessPartners =
            webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody(object :
                ParameterizedTypeReference<PageResponse<BusinessPartnerResponse>>() {})
                .returnResult().responseBody

        assertThat(savedBusinessPartners!!.content.map(::extractCdqId)).containsExactlyInAnyOrder("fooId1", "fooId2")
    }

    @Test
    @DirtiesContext
    fun `given new partners in cdq, when import partners multiple times, then no duplicate partners`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners")).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(readTestResource("cdq/business-partners.json"))
            )
        )

        val importResult = webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful
            .expectBody(ImportResponse::class.java)
            .returnResult()
            .responseBody!!

        assertThat(importResult.importedSize == 2)
        assertThat(importResult.partnerBpns.size == 2)

        webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody()
            .jsonPath("$.totalElements").isEqualTo(2)

        // importing same data again does not create new business partners
        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(ImportResponse::class.java)
            .returnResult()
            .responseBody!!

        assertThat(importResult.importedSize == 0)
        assertThat(importResult.partnerBpns.isEmpty())

        webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody()
            .jsonPath("$.totalElements").isEqualTo(2)
    }

    @Test
    @DirtiesContext
    fun `given new partners in cdq, when import partners with pagination, then partners imported`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners-paginated-1.json"))
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo("fooNextStartAfterId1"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners-paginated-2.json"))
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo("fooNextStartAfterId2"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners-paginated-3.json"))
                )
        )

        val importResult = webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBody(ImportResponse::class.java)
            .returnResult()
            .responseBody!!

        assertThat(importResult.importedSize == 2)
        assertThat(importResult.partnerBpns.size == 2)

        val savedBusinessPartners =
            webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody(object :
                ParameterizedTypeReference<PageResponse<BusinessPartnerResponse>>() {})
                .returnResult().responseBody

        assertThat(savedBusinessPartners!!.content.map(::extractCdqId)).containsExactlyInAnyOrder("fooId1", "fooId2")

        wireMockServer.verify(3, getRequestedFor(urlPathMatching("$CDQ_MOCK_URL/businesspartners")))
    }

    private fun readTestResource(testResourcePath: String) =
        CdqControllerImportIT::class.java.classLoader.getResource(testResourcePath)!!.readText()

    private fun extractCdqId(it: BusinessPartnerResponse) = it.identifiers.find { id -> id.type.technicalKey == cdqIdProperties.typeKey }!!.value
}