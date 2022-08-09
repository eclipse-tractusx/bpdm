package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.AugmentedBusinessPartnerResponseCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_OUTPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_DATA_CLINIC_API_PATH
import org.eclipse.tractusx.bpdm.gate.util.ResponseValues
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
internal class LegalEntityControllerOutputIT @Autowired constructor(
    val webTestClient: WebTestClient,
    val objectMapper: ObjectMapper,
    val cdqConfigProperties: CdqConfigProperties
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

    /**
     * Given legal entity exists in cdq
     * When getting legal entity by external id via output route
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id`() {
        val expectedLegalEntity = ResponseValues.legalEntityGateOutput1

        wireMockServer.stubFor(
            post(urlPathMatching(getReadAugmentedBusinessPartnerPath()))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                AugmentedBusinessPartnerResponseCdq(CdqValues.legalEntity1Response)
                            )
                        )
                )
        )

        val legalEntity = webTestClient.get().uri(CATENA_OUTPUT_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1Response.externalId}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(LegalEntityGateOutput::class.java)
            .returnResult()
            .responseBody

        assertThat(legalEntity).usingRecursiveComparison().isEqualTo(expectedLegalEntity)
    }

    /**
     * Given legal entity does not exist in cdq
     * When getting legal entity by external id via output route
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {
        wireMockServer.stubFor(
            post(urlPathMatching(getReadAugmentedBusinessPartnerPath()))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                )
        )

        webTestClient.get().uri("$CATENA_OUTPUT_LEGAL_ENTITIES_PATH/nonexistent-externalid123")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    /**
     * When cdq api responds with an error status code while fetching legal entity by external id
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entity by external id, cdq error`() {
        wireMockServer.stubFor(
            post(urlPathMatching(getReadAugmentedBusinessPartnerPath()))
                .willReturn(badRequest())
        )

        webTestClient.get().uri("$CATENA_OUTPUT_LEGAL_ENTITIES_PATH/some-externalid123")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given legal entity without legal address in CDQ
     * When query by its external ID
     * Then server error is returned
     */
    @Test
    fun `get legal entity without legal address, expect error`() {
        val invalidPartner = CdqValues.legalEntity1.copy(addresses = emptyList())

        wireMockServer.stubFor(
            post(urlPathMatching(getReadAugmentedBusinessPartnerPath()))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                AugmentedBusinessPartnerResponseCdq(invalidPartner)
                            )
                        )
                )
        )

        webTestClient.get().uri(CATENA_OUTPUT_LEGAL_ENTITIES_PATH + "/${CdqValues.legalEntity1Response.externalId}")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * Given legal entities exists in cdq
     * When getting legal entities page via output route
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entities`() {
        val legalEntitiesCdq = listOf(
            CdqValues.legalEntity1Response,
            CdqValues.legalEntity2Response
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutput1,
            ResponseValues.legalEntityGateOutput2
        )

        val limit = 2
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 0

        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = legalEntitiesCdq.map { AugmentedBusinessPartnerResponseCdq(it) }
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_OUTPUT_LEGAL_ENTITIES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<LegalEntityGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedLegalEntities,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * Given legal entity without legal address in CDQ
     * When getting legal entities page via output route
     * Then only valid legal entities on page returned
     */
    @Test
    fun `filter legal entities without legal address`() {
        val legalEntitiesCdq = listOf(
            CdqValues.legalEntity1Response,
            CdqValues.legalEntity2Response,
            CdqValues.legalEntity1Response.copy(addresses = emptyList())
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutput1,
            ResponseValues.legalEntityGateOutput2
        )

        val limit = 3
        val startAfter = "Aaa111"
        val nextStartAfter = "Aaa222"
        val total = 10
        val invalidEntries = 1

        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            objectMapper.writeValueAsString(
                                PagedResponseCdq(
                                    limit = limit,
                                    startAfter = startAfter,
                                    nextStartAfter = nextStartAfter,
                                    total = total,
                                    values = legalEntitiesCdq.map { AugmentedBusinessPartnerResponseCdq(it) }
                                )
                            )
                        )
                )
        )

        val pageResponse = webTestClient.get()
            .uri { builder ->
                builder.path(CATENA_OUTPUT_LEGAL_ENTITIES_PATH)
                    .queryParam(PaginationStartAfterRequest::startAfter.name, startAfter)
                    .queryParam(PaginationStartAfterRequest::limit.name, limit)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<PageStartAfterResponse<LegalEntityGateOutput>>()
            .responseBody
            .blockFirst()!!

        assertThat(pageResponse).isEqualTo(
            PageStartAfterResponse(
                total = total,
                nextStartAfter = nextStartAfter,
                content = expectedLegalEntities,
                invalidEntries = invalidEntries
            )
        )
    }

    /**
     * When cdq api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, cdq error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        webTestClient.get().uri(CATENA_OUTPUT_LEGAL_ENTITIES_PATH)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    /**
     * When cdq api responds with an error status code while getting legal entities
     * Then an internal server error response should be sent
     */
    @Test
    fun `get legal entities, pagination limit exceeded`() {
        webTestClient.get().uri { builder ->
            builder.path(CATENA_OUTPUT_LEGAL_ENTITIES_PATH)
                .queryParam(PaginationStartAfterRequest::limit.name, 999999)
                .build()
        }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    private fun getReadAugmentedBusinessPartnerPath(): String {
        return "$CDQ_MOCK_DATA_CLINIC_API_PATH/datasources/${cdqConfigProperties.datasource}/augmentedbusinesspartners/fetch"
    }
}