/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.gate.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.config.TypeMatchConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.response.LsaType
import org.eclipse.tractusx.bpdm.gate.dto.response.TypeMatchResponse
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues
import org.eclipse.tractusx.bpdm.gate.util.RequestValues
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BusinessPartnerControllerIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper,
    private val typeMatchConfigProperties: TypeMatchConfigProperties
) {

    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }

    /**
     * Given business partner candidate whose properties are matching existing legal entity
     * When invoking type match for candidate
     * Then return candidate is type of legal entity
     */
    @Test
    fun `match legal entity type`() {
        //make sure candidate is valid by providing identifier and name
        val givenCandidate = with(RequestValues.candidate1) {
            copy(
                identifiers = listOf(RequestValues.identifier1),
                names = listOf(RequestValues.name1)
            )
        }

        val expectedScore = typeMatchConfigProperties.legalEntityThreshold
        val expected = TypeMatchResponse(expectedScore, LsaType.LegalEntity)

        setLookupMockResponse(expectedScore)

        val typeResponse = webTestClient.post().uri(EndpointValues.GATE_API_TYPE_MATCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(givenCandidate))
            .exchange()
            .expectStatus().isOk
            .returnResult<TypeMatchResponse>()
            .responseBody
            .blockFirst()

        assertThat(typeResponse).isEqualTo(expected)
    }

    /**
     * Given business partner candidate whose properties are not matching known legal entity
     * When invoking type match for candidate
     * Then return candidate is of no type
     */
    @Test
    fun `match no type`() {
        //make sure candidate is valid by providing identifier and name
        val givenCandidate = with(RequestValues.candidate1) {
            copy(
                identifiers = listOf(RequestValues.identifier1),
                names = listOf(RequestValues.name1)
            )
        }

        val mockedScore = (typeMatchConfigProperties.legalEntityThreshold - 1.0f).coerceAtLeast(0.0f)
        val expected = TypeMatchResponse(1f - mockedScore, LsaType.None)

        setLookupMockResponse(mockedScore)

        val typeResponse = webTestClient.post().uri(EndpointValues.GATE_API_TYPE_MATCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(givenCandidate))
            .exchange()
            .expectStatus().isOk
            .returnResult<TypeMatchResponse>()
            .responseBody
            .blockFirst()

        assertThat(typeResponse).isEqualTo(expected)
    }

    /**
     * Given business partner candidate without name
     * When invoking type match for candidate
     * Then candidate accepted for type matching
     */
    @Test
    fun `accept candidate without name`() {
        //create candidate without name
        val givenCandidate = with(RequestValues.candidate1) {
            copy(
                identifiers = listOf(RequestValues.identifier1),
                names = emptyList()
            )
        }

        setLookupMockResponse(0.5f)

        webTestClient.post().uri(EndpointValues.GATE_API_TYPE_MATCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(givenCandidate))
            .exchange()
            .expectStatus().isOk
    }

    /**
     * Given business partner candidate without identifier
     * When invoking type match for candidate
     * Then candidate accepted for type matching
     */
    @Test
    fun `accept candidate without identifier`() {
        //create candidate without identifier
        val givenCandidate = with(RequestValues.candidate1) {
            copy(
                identifiers = emptyList(),
                names = listOf(RequestValues.name1)
            )
        }

        setLookupMockResponse(0.5f)

        webTestClient.post().uri(EndpointValues.GATE_API_TYPE_MATCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(givenCandidate))
            .exchange()
            .expectStatus().isOk
    }

    /**
     * Given business partner candidate without identifier and name
     * When invoking type match for candidate
     * Then return 400
     */
    @Test
    fun `refuse candidate without name and identifier`() {
        //create candidate without identifier
        val givenCandidate = with(RequestValues.candidate1) {
            copy(
                identifiers = emptyList(),
                names = emptyList()
            )
        }

        setLookupMockResponse(0.5f)

        webTestClient.post().uri(EndpointValues.GATE_API_TYPE_MATCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(givenCandidate))
            .exchange()
            .expectStatus().is4xxClientError
    }

    private fun setLookupMockResponse(overallScore: Float) {
        val mockedResponse = ReferenceDataLookupResponseSaas(
            1, 1, 0, 1,
            listOf(
                BusinessPartnerLookupMatchSaas(
                    "0", "0",
                    MatchingProfileSaas(
                        MatchingScoresSaas(MatchingScoreSaas(0.5f), MatchingScoreSaas(0.5f), MatchingScoreSaas(overallScore))
                    )
                )
            )
        )

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching(EndpointValues.SAAS_MOCK_REFERENCE_DATA_LOOKUP_PATH))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(mockedResponse))
                )
        )
    }


}