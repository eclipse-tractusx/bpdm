package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.cdq.AugmentedBusinessPartnerResponseCdq
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.util.CdqValues
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CATENA_OUTPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.CDQ_MOCK_STORAGE_PATH
import org.eclipse.tractusx.bpdm.gate.util.ResponseValues
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
     * Given augmented business partner exists in cdq
     * When getting legal entity by external id with output route
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id from output route`() {
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

    private fun getReadAugmentedBusinessPartnerPath(): String {
        return "$CDQ_MOCK_STORAGE_PATH/datasources/${cdqConfigProperties.datasource}/augmentedbusinesspartners/fetch"
    }
}