/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.request.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityGateInputDto
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.GATE_API_INPUT_LEGAL_ENTITIES_PATH
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["bpdm.api.upsert-limit=2"])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class LegalEntityControllerInputIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    private val webTestClient: WebTestClient,
    private val objectMapper: ObjectMapper,
    val gateClient: GateClient,
    private val legalEntityRepository: LegalEntityRepository,
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }


    /**
     * When upserting legal entities
     * Then legal entity should be persisted on the database
     */
    @Test
    fun `upsert legal entities`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2,
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted Address data
        val legalEntityExternal1 = legalEntityRepository.findByExternalId("external-1")
        assertNotEquals(legalEntityExternal1, null)

        val legalEntityExternal2 = legalEntityRepository.findByExternalId("external-2")
        assertNotEquals(legalEntityExternal2, null)

    }

    /**
     * When upserting legal entities with missing external id
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, missing external id`() {
        val legalEntitiesJson: JsonNode = objectMapper.createArrayNode().add(
            objectMapper.valueToTree<ObjectNode>(BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1)
                .apply { remove(LegalEntityGateInputRequest::externalId.name) }
        )

        webTestClient.put().uri(GATE_API_INPUT_LEGAL_ENTITIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(legalEntitiesJson.toString())
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    /**
     * When upper limit of legal entities in a request is exceeded when upserting legal entities
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, legal entity limit exceeded`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1.copy(externalId = "external-1"),
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1.copy(externalId = "external-2")
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }
    }

    /**
     * When upserting legal entities with duplicate external ids within the same request
     * Then a bad request response should be sent
     */
    @Test
    fun `upsert legal entities, duplicate external id`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1.copy()
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entity exists
     * When getting legal entity by external id
     * Then legal entity mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity by external id`() {
        val expectedLegalEntity = BusinessPartnerVerboseValues.legalEntityGateInputResponse1

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        val legalEntity = gateClient.legalEntities.getLegalEntityByExternalId(BusinessPartnerVerboseValues.externalId1)

        assertLegalEntitiesEqual(legalEntity, expectedLegalEntity)

    }

    /**
     * Given legal entity does not exist
     * When getting legal entity by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get legal entity by external id, not found`() {

        try {
            gateClient.legalEntities.getLegalEntityByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }


    /**
     * Given legal entity exists in the persistence database
     * When getting legal entities page
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entities`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val expectedLegalEntities = listOf(
            BusinessPartnerVerboseValues.legalEntityGateInputResponse1,
            BusinessPartnerVerboseValues.legalEntityGateInputResponse2,
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2


        val paginationValue = PaginationRequest(page, size)
        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        val pageResponse = gateClient.legalEntities.getLegalEntities(paginationValue)

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )

        assertPageLegalEntitiesEqual(pageResponse, expectedPage)
    }


    /**
     * Given legal entity exists in the peristence database
     * When getting legal entities page based on external id list
     * Then legal entities page mapped to the catena data model should be returned
     */
    @Test
    fun `get legal entity filter by external ids`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val expectedLegalEntities = listOf(
            BusinessPartnerVerboseValues.legalEntityGateInputResponse1,
            BusinessPartnerVerboseValues.legalEntityGateInputResponse2,
        )
        val page = 0
        val size = 10
        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2

        val paginationValue = PaginationRequest(page, size)
        gateClient.legalEntities.upsertLegalEntities(legalEntities)

        val listExternalIds = legalEntities.map { it.externalId }

        val pageResponse = gateClient.legalEntities.getLegalEntitiesByExternalIds(paginationValue, listExternalIds)

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )

        assertPageLegalEntitiesEqual(pageResponse, expectedPage)
    }

    /**
     * When requesting too many legal entities
     * Then a bad request response should be sent
     */
    @Test
    fun `get legal entities, pagination limit exceeded`() {

        try {
            val paginationValue = PaginationRequest(0, 999999)
            gateClient.legalEntities.getLegalEntities(paginationValue)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * When upserting legal entities
     * Then SaaS upsert api should be called with the legal entity data mapped to the SaaS data model
     */
    @Test
    fun `upsert and persist legal entities`() {
        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2,
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        val legalEntityRecordExternal1 = legalEntityRepository.findByExternalId("external-1")
        assertNotEquals(legalEntityRecordExternal1, null)

        val legalEntityRecordExternal2 = legalEntityRepository.findByExternalId("external-2")
        assertNotEquals(legalEntityRecordExternal2, null)

    }

    private fun assertPageLegalEntitiesEqual(actual: PageDto<LegalEntityGateInputDto>, expected: PageDto<LegalEntityGateInputDto>) =
        testHelpers.assertRecursively(actual).isEqualTo(expected)

    private fun assertLegalEntitiesEqual(actual: LegalEntityGateInputDto, expected: LegalEntityGateInputDto) =
        testHelpers.assertRecursively(actual).isEqualTo(expected)

}