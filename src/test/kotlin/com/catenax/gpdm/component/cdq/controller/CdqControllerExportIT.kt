package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.dto.IdentifierCdq
import com.catenax.gpdm.component.cdq.dto.TypeKeyNameUrlCdq
import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.service.BusinessPartnerService
import com.catenax.gpdm.service.IdentifierService
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
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
import java.time.LocalDateTime


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CdqControllerExportIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val cdqIdProperties: CdqIdentifierConfigProperties,
    val bpnConfigProperties: BpnConfigProperties,
    val businessPartnerService: BusinessPartnerService,
    val objectMapper: ObjectMapper
) {

    // use spy since service calls should only actually be mocked in some tests while real service calls should be used in other tests
    @SpykBean
    lateinit var identifierService: IdentifierService

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
    fun `given partners unsynchronized, when export, then partners synchronized`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners-minimal.json"))
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
    fun `given partners unsynchronized, when cdq error on export, then partners still unsynchronized`() {
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readTestResource("cdq/business-partners-minimal.json"))
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

    @Test
    @DirtiesContext
    fun `given partners unsynchronized, when error saving synchronized state after export and triggering another export, then partners synchronized`() {
        // mock error while saving synchronized state on first export
        // second export should work though, so call real method
        every { identifierService.updateIdentifiers(any(), any()) } throws RuntimeException() andThenAnswer { callOriginal() }

        // at first, cdq returns business partners without bpn
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                createBusinessPartners(
                                    mapOf(
                                        "fooId1" to null,
                                        "fooId2" to null
                                    )
                                )
                            )
                        )
                )
        )

        // put request with created bpns to cdq should still occur since mocked error occurs afterwards
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

        // mocked error should occur when trying to save synchronized state
        webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is5xxServerError

        val importedBusinessPartners = businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)

        // since cdq should have received bpns, mock that bpns are now included when business partners are retrieved from cdq
        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                createBusinessPartners(
                                    mapOf(
                                        "fooId1" to importedBusinessPartners.map { it.bpn }[0],
                                        "fooId2" to importedBusinessPartners.map { it.bpn }[1]
                                    )
                                )
                            )
                        )
                )
        )

        // now nothing should be exported to cdq since business partners from cdq already contain bpns
        val exportedBusinessPartners = webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBodyList(BusinessPartnerCdq::class.java).returnResult().responseBody

        assertThat(exportedBusinessPartners).isEmpty()

        // business partners should be in state "synchronized" now since cdq api returned them with bpns
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)).hasSize(0)
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusSynchronizedKey)).hasSize(2)
    }

    private fun createBusinessPartners(cdqIdToBpn: Map<String, String?>): BusinessPartnerCollectionCdq {
        val businessPartners = cdqIdToBpn.map { entry ->
            val cdqId = entry.key
            val bpn = entry.value

            var bpnIdentifier: IdentifierCdq? = null
            if (bpn != null) {
                bpnIdentifier = IdentifierCdq(
                    type = TypeKeyNameUrlCdq(technicalKey = bpnConfigProperties.id, name = bpnConfigProperties.name),
                    value = bpn
                )
            }

            BusinessPartnerCdq(
                createdAt = LocalDateTime.of(2020, 1, 1, 0, 0),
                lastModifiedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
                dataSource = "fooDataSource",
                id = cdqId,
                identifiers = if (bpnIdentifier != null) listOf(bpnIdentifier) else emptyList()
            )
        }

        return BusinessPartnerCollectionCdq(limit = businessPartners.size, total = businessPartners.size, values = businessPartners)
    }

    private fun readTestResource(testResourcePath: String) =
        CdqControllerExportIT::class.java.classLoader.getResource(testResourcePath)!!.readText()

    private fun extractBpn(it: BusinessPartnerCdq) = it.identifiers.find { id -> id.type?.technicalKey == bpnConfigProperties.id }?.value
}