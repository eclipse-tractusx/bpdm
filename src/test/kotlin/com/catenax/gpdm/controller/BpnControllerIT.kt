package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.component.cdq.service.ImportStarterService
import com.catenax.gpdm.dto.request.IdentifiersSearchRequest
import com.catenax.gpdm.dto.response.BpnSearchResponse
import com.catenax.gpdm.util.CdqValues
import com.catenax.gpdm.util.EndpointValues
import com.catenax.gpdm.util.TestHelpers
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
class BpnControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val importService: ImportStarterService,
    val objectMapper: ObjectMapper,
    val webTestClient: WebTestClient,
    val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
) {
    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    val partnerDocs = listOf(
        CdqValues.businessPartner1,
        CdqValues.businessPartner2,
        CdqValues.businessPartner3
    )

    @BeforeEach
    fun beforeEach() {
        val importCollection = BusinessPartnerCollectionCdq(
            partnerDocs.size,
            null,
            null,
            partnerDocs.size,
            partnerDocs
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching(EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(importCollection))
                )
        )

        importService.import()
    }

    @AfterEach
    fun afterEach() {
        testHelpers.truncateH2()
    }

    @Test
    fun findBpnsByIdentifiers() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(cdqIdentifierConfigProperties.typeKey, listOf(CdqValues.businessPartner1.id, CdqValues.businessPartner2.id))

        val bpnSearchResponses = webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BpnSearchResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(bpnSearchResponses!!.map { it.idValue }).containsExactlyInAnyOrder(CdqValues.businessPartner1.id, CdqValues.businessPartner2.id)
    }
}