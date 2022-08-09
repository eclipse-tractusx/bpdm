package org.eclipse.tractusx.bpdm.pool.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.service.ImportStarterService
import org.eclipse.tractusx.bpdm.pool.dto.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.util.CdqValues
import org.eclipse.tractusx.bpdm.pool.util.EndpointValues
import org.eclipse.tractusx.bpdm.pool.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class],
    properties = ["bpdm.bpn.search-request-limit=2"]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class BpnControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val importService: ImportStarterService,
    val objectMapper: ObjectMapper,
    val webTestClient: WebTestClient,
    val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
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

    private val partnerDocs = listOf(
        CdqValues.businessPartner1,
        CdqValues.businessPartner2,
        CdqValues.businessPartner3
    )

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()

        val importCollection = PagedResponseCdq(
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

    /**
     * Given some business partners imported
     * When requesting bpn to CDQ id mappings and all the requested CDQ ids exist in the db
     * Then all the requested mappings are returned
     */
    @Test
    fun `find bpns by identifiers, all found`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(cdqIdentifierConfigProperties.typeKey, listOf(CdqValues.businessPartner1.id!!, CdqValues.businessPartner2.id!!))

        val bpnIdentifierMappings = webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BpnIdentifierMappingResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(CdqValues.businessPartner1.id, CdqValues.businessPartner2.id)
    }

    /**
     * Given some business partners imported
     * When requesting bpn to CDQ id mappings and only some of the requested CDQ ids exist in the db
     * Then only the requested mappings that exist in the db are returned
     */
    @Test
    fun `find bpns by identifiers, only some found`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(cdqIdentifierConfigProperties.typeKey, listOf(CdqValues.businessPartner1.id!!, "someNonexistentCdqId"))

        val bpnIdentifierMappings = webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BpnIdentifierMappingResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(CdqValues.businessPartner1.id)
    }

    /**
     * Given some business partners imported
     * When requesting too many bpn to CDQ id mappings in a single request, so that the requested number exceeds the configured limit
     * Then a "bad request" response is sent
     */
    @Test
    fun `find bpns by identifiers, bpn request limit exceeded`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(
                cdqIdentifierConfigProperties.typeKey,
                listOf(CdqValues.businessPartner1.id!!, CdqValues.businessPartner2.id!!, CdqValues.businessPartner3.id!!)
            )

        webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * Given some business partners imported
     * When requested identifier type not found
     * Then a "not found" response is sent
     */
    @Test
    fun `find bpns by nonexistent identifier type`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest("NONEXISTENT_IDENTIFIER_TYPE", listOf(CdqValues.businessPartner1.id!!))

        webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isNotFound
    }
}