package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.service.BusinessPartnerService
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
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
class CdqControllerExportIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val cdqIdProperties: CdqIdentifierConfigProperties,
    val bpnConfigProperties: BpnConfigProperties,
    val businessPartnerService: BusinessPartnerService
) {

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

        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerResponse::class.java).hasSize(2)

        webTestClient.get().uri("/api/catena/business-partner").exchange().expectStatus().isOk.expectBody()
            .jsonPath("$.totalElements").isEqualTo(2)

        val importedBusinessPartners = businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)

        // business partners in cdq should be updated with newly created bpns
        wireMockServer.stubFor(
            put(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withRequestBody(matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[0]}\")]"))
                .withRequestBody(matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[1]}\")]"))
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

        val exportedBusinessPartners = webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerCdq::class.java).returnResult().responseBody

        assertThat(exportedBusinessPartners).hasSize(2)
        assertThat(exportedBusinessPartners!!.map(::extractBpn)).containsExactlyInAnyOrderElementsOf(importedBusinessPartners.map { it.bpn })
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)).isEmpty()
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusSynchronizedKey)).hasSize(2)
    }

    @Test
    @DirtiesContext
    fun `cdq returns error on export`() {
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
                    aResponse().withStatus(500)
                )
        )

        webTestClient.post().uri("/api/cdq/business-partners/import")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerResponse::class.java).hasSize(2)

        val importedBusinessPartners = businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)

        // try export, cdq returns error
        webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is5xxServerError

        // try export again
        webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is5xxServerError

        // cdq export should have been called both times
        wireMockServer.verify(
            2, putRequestedFor(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .withRequestBody(matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[0]}\")]"))
                .withRequestBody(matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[1]}\")]"))
        )

        // imported business partners should still be in state "imported" (not synchronized)
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)).hasSize(2)
    }

    private fun readTestResource(testResourcePath: String) =
        CdqControllerExportIT::class.java.classLoader.getResource(testResourcePath)!!.readText()

    private fun extractBpn(it: BusinessPartnerCdq) = it.identifiers.find { id -> id.type?.technicalKey == bpnConfigProperties.id }?.value
}