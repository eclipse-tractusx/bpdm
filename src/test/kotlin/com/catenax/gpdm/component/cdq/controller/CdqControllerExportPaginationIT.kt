package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.dto.ExportResponse
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import com.catenax.gpdm.service.BusinessPartnerService
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.LocalDateTime


private const val CDQ_MOCK_URL = "/test-cdq-api/storages/test-cdq-storage"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.cdq.export-page-size=1"])
@ActiveProfiles("test")
class CdqControllerExportPaginationIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val cdqIdProperties: CdqIdentifierConfigProperties,
    val businessPartnerService: BusinessPartnerService,
    val importService: PartnerImportService,
    val objectMapper: ObjectMapper
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
    fun `given partners unsynchronized, when paginated export, then partners synchronized`() {
        val businessPartnersCdq = listOf(
            createBusinessPartner("cdqid-1"),
            createBusinessPartner("cdqid-2")
        )

        wireMockServer.stubFor(
            get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                BusinessPartnerCollectionCdq(
                                    limit = businessPartnersCdq.size,
                                    total = businessPartnersCdq.size,
                                    values = businessPartnersCdq
                                )
                            )
                        )
                )
        )

        importService.import()

        val importedBusinessPartners = businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey).toList()

        // business partners in cdq should be updated with newly created bpns
        wireMockServer.stubFor(
            put(urlPathMatching("$CDQ_MOCK_URL/businesspartners")).withRequestBody(
                matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[0]}\")]")
                    .or(matchingJsonPath("$..identifiers[?(@.value==\"${importedBusinessPartners.map { it.bpn }[1]}\")]"))
            ).willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                               "numberOfAccepted": 1,
                               "numberOfFailed": 0,
                               "featuresOn": [],
                               "failures": []
                            }
                        """.trimIndent()
                    )
            )
        )

        for (businessPartnerCdq in businessPartnersCdq) {
            wireMockServer.stubFor(
                get(urlPathMatching("$CDQ_MOCK_URL/businesspartners"))
                    .withQueryParam("businessPartnerId", equalTo(businessPartnerCdq.id))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                objectMapper.writeValueAsString(
                                    BusinessPartnerCollectionCdq(
                                        limit = 1,
                                        total = 1,
                                        values = listOf(businessPartnerCdq)
                                    )
                                )
                            )
                    )
            )
        }


        val exportResponse = webTestClient.post().uri("/api/cdq/business-partners/export")
            .exchange()
            .expectStatus()
            .is2xxSuccessful.expectBody(ExportResponse::class.java).returnResult().responseBody!!

        assertThat(exportResponse.exportedSize).isEqualTo(2)
        assertThat(exportResponse.partnerBpns.size).isEqualTo(2)
        assertThat(exportResponse.partnerBpns).containsExactlyInAnyOrderElementsOf(importedBusinessPartners.map { it.bpn })
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusImportedKey)).isEmpty()
        assertThat(businessPartnerService.findPartnersByIdentifier(cdqIdProperties.typeKey, cdqIdProperties.statusSynchronizedKey)).hasSize(2)
    }

    private fun createBusinessPartner(cdqId: String): BusinessPartnerCdq {
        val datasource1 = "datasource"
        val timestamp = LocalDateTime.of(2022, 1, 1, 0, 0)
        return BusinessPartnerCdq(id = cdqId, createdAt = timestamp, lastModifiedAt = timestamp, dataSource = datasource1)
    }
}