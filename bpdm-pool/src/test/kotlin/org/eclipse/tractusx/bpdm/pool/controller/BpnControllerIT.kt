/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.dto.request.IdentifiersSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.util.*
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
    val objectMapper: ObjectMapper,
    val webTestClient: WebTestClient,
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

    val identifierType = RequestValues.legalEntityCreate1.properties.identifiers.first().type
    val identifierValue1 = RequestValues.legalEntityCreate1.properties.identifiers.first().value
    val identifierValue2 = RequestValues.legalEntityCreate2.properties.identifiers.first().value
    val identifierValue3 = RequestValues.legalEntityCreate3.properties.identifiers.first().value

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata(webTestClient)
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3),
            ),
            webTestClient
        )
    }

    /**
     * Given some business partners imported
     * When requesting bpn to CDQ id mappings and all the requested CDQ ids exist in the db
     * Then all the requested mappings are returned
     */
    @Test
    fun `find bpns by identifiers, all found`() {
        val identifiersSearchRequest = IdentifiersSearchRequest(identifierType, listOf(identifierValue1, identifierValue2))

        val bpnIdentifierMappings = webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BpnIdentifierMappingResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(identifierValue1, identifierValue2)
    }

    /**
     * Given some business partners imported
     * When requesting bpn to CDQ id mappings and only some of the requested CDQ ids exist in the db
     * Then only the requested mappings that exist in the db are returned
     */
    @Test
    fun `find bpns by identifiers, only some found`() {
        val identifiersSearchRequest =
            IdentifiersSearchRequest(identifierType, listOf(identifierValue1, "someNonexistentCdqId"))

        val bpnIdentifierMappings = webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BpnIdentifierMappingResponse::class.java)
            .returnResult()
            .responseBody

        assertThat(bpnIdentifierMappings!!.map { it.idValue }).containsExactlyInAnyOrder(identifierValue1)
    }

    /**
     * Given some business partners imported
     * When requesting too many bpn to CDQ id mappings in a single request, so that the requested number exceeds the configured limit
     * Then a "bad request" response is sent
     */
    @Test
    fun `find bpns by identifiers, bpn request limit exceeded`() {
        val identifiersSearchRequest = IdentifiersSearchRequest(identifierType, listOf(identifierValue1, identifierValue2, identifierValue3))

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
        val identifiersSearchRequest = IdentifiersSearchRequest("NONEXISTENT_IDENTIFIER_TYPE", listOf(identifierValue1))

        webTestClient.post().uri(EndpointValues.CATENA_BPN_SEARCH_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(identifiersSearchRequest))
            .exchange()
            .expectStatus()
            .isNotFound
    }
}