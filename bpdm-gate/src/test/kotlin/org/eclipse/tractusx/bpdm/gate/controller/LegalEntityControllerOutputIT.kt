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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class LegalEntityControllerOutputIT @Autowired constructor(
    val gateClient: GateClient,
    private val gateLegalEntityRepository: LegalEntityRepository,
    private val testHelpers: DbTestHelpers
) {
    companion object {
        @RegisterExtension
        private val wireMockServerSaas: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @RegisterExtension
        private val wireMockServerBpdmPool: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServerSaas.baseUrl() }
            registry.add("bpdm.pool.base-url") { wireMockServerBpdmPool.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * If there is an Input Legal Entity persisted,
     * upsert the Output with same external id
     */
    @Test
    fun `upsert output legal entities`() {

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2,
        )

        val legalEntitiesOutput = listOf(
            RequestValues.legalEntityGateOutputRequest1,
            RequestValues.legalEntityGateOutputRequest2,
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
            gateClient.legalEntities().upsertLegalEntitiesOutput(legalEntitiesOutput)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted Address data
        val legalEntityExternal1 = gateLegalEntityRepository.findByExternalIdAndDataType(CommonValues.externalId1, OutputInputEnum.Output)
        Assertions.assertNotEquals(legalEntityExternal1, null)

        val legalEntityExternal2 = gateLegalEntityRepository.findByExternalIdAndDataType(CommonValues.externalId2, OutputInputEnum.Output)
        Assertions.assertNotEquals(legalEntityExternal2, null)

    }

    /**
     * If there isn't an Input Legal Entity persisted,
     * when upserting an output Legal Entity, it should show an 400
     */
    @Test
    fun `upsert output legal entities, no input persisted`() {

        val legalEntitiesOutput = listOf(
            RequestValues.legalEntityGateOutputRequest1,
            RequestValues.legalEntityGateOutputRequest2,
        )

        try {
            gateClient.legalEntities().upsertLegalEntitiesOutput(legalEntitiesOutput)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entities exists in the database
     * When getting legal entities page via output route
     * Then legal entities page should be returned
     */
    @Test
    fun `get legal entities output`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2,
        )

        val legalEntitiesOutput = listOf(
            RequestValues.legalEntityGateOutputRequest1,
            RequestValues.legalEntityGateOutputRequest2,
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutputResponse1,
            ResponseValues.legalEntityGateOutputResponse2,
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2


        val paginationValue = PaginationRequest(page, size)

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        gateClient.legalEntities().upsertLegalEntitiesOutput(legalEntitiesOutput)

        val pageResponse = gateClient.legalEntities().getLegalEntitiesOutput(paginationValue, emptyList())

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )

        assertThat(pageResponse).usingRecursiveComparison().ignoringCollectionOrder().ignoringAllOverriddenEquals()
            .ignoringFieldsMatchingRegexes(".*processStartedAt*", ".*administrativeAreaLevel1*.")
            .isEqualTo(expectedPage)
    }

    /**
     * Given legal entities exists in the database
     * When getting legal entities page via output route filtering by external ids
     * Then legal entities page should be returned
     */
    @Test
    fun `get legal entities, filter by external ids`() {
        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2,
        )

        val legalEntitiesOutput = listOf(
            RequestValues.legalEntityGateOutputRequest1,
            RequestValues.legalEntityGateOutputRequest2,
        )

        val expectedLegalEntities = listOf(
            ResponseValues.legalEntityGateOutputResponse1,
            ResponseValues.legalEntityGateOutputResponse2,
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2


        val paginationValue = PaginationRequest(page, size)

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        gateClient.legalEntities().upsertLegalEntitiesOutput(legalEntitiesOutput)

        val pageResponse = gateClient.legalEntities().getLegalEntitiesOutput(paginationValue, listOf(CommonValues.externalId1, CommonValues.externalId2))

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedLegalEntities
        )

        assertThat(pageResponse).usingRecursiveComparison().ignoringCollectionOrder().ignoringAllOverriddenEquals()
            .ignoringFieldsMatchingRegexes(".*processStartedAt*", ".*administrativeAreaLevel1*.")
            .isEqualTo(expectedPage)
    }
}