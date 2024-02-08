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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
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
internal class SiteControllerOutputIT @Autowired constructor(
    val gateClient: GateClient,
    private val gateSiteRepository: SiteRepository,
    val testHelpers: DbTestHelpers
) {
    companion object {

        @RegisterExtension
        private val wireMockServerBpdmPool: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.client.pool.base-url") { wireMockServerBpdmPool.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * If there is an Input Sites persisted,
     * upsert the Output with same external id
     */
    @Test
    fun `upsert sites output`() {
        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        val sitesOutput = listOf(
            BusinessPartnerNonVerboseValues.siteGateOutputRequest1,
            BusinessPartnerNonVerboseValues.siteGateOutputRequest2
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val legalEntitiesOutput = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest2,
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
            gateClient.legalEntities.upsertLegalEntitiesOutput(legalEntitiesOutput)
            gateClient.sites.upsertSites(sites)
            gateClient.sites.upsertSitesOutput(sitesOutput)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted site data
        val siteExternal1 = gateSiteRepository.findByExternalIdAndStage("site-external-1", StageType.Output)
        Assertions.assertNotEquals(siteExternal1, null)

        val siteExternal2 = gateSiteRepository.findByExternalIdAndStage("site-external-2", StageType.Output)
        Assertions.assertNotEquals(siteExternal2, null)

    }

    /**
     * If there isn't an Input Sites persisted,
     * when upserting an output Sites, it should show an 400
     */
    @Test
    fun `upsert sites output, no input persisted`() {

        val sitesOutput = listOf(
            BusinessPartnerNonVerboseValues.siteGateOutputRequest1,
            BusinessPartnerNonVerboseValues.siteGateOutputRequest2
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val legalEntitiesOutput = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest2,
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
            gateClient.legalEntities.upsertLegalEntitiesOutput(legalEntitiesOutput)
            gateClient.sites.upsertSitesOutput(sitesOutput)
        } catch (e: WebClientResponseException) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given sites exists in the database
     * When getting sites page via output route
     * Then sites page should be returned
     */
    @Test
    fun `get sites`() {
        val expectedSites = listOf(
            BusinessPartnerVerboseValues.persistencesiteGateOutputResponse1,
            BusinessPartnerVerboseValues.persistencesiteGateOutputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        val sitesOutput = listOf(
            BusinessPartnerNonVerboseValues.siteGateOutputRequest1,
            BusinessPartnerNonVerboseValues.siteGateOutputRequest2
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val legalEntitiesOutput = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest2,
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        gateClient.legalEntities.upsertLegalEntitiesOutput(legalEntitiesOutput)
        gateClient.sites.upsertSites(sites)
        gateClient.sites.upsertSitesOutput(sitesOutput)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites.getSitesOutput(paginationValue, emptyList())

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*administrativeAreaLevel1*", ".*processStartedAt*").isEqualTo(
            PageDto(
                totalElements = totalElements,
                totalPages = totalPages,
                page = pageValue,
                contentSize = contentSize,
                content = expectedSites
            )
        )
    }

    /**
     * Given sites exists in the database
     * When getting sites page via output route filtering by external ids
     * Then sites page should be returned
     */
    @Test
    fun `get sites, filter by external ids`() {
        val expectedSites = listOf(
            BusinessPartnerVerboseValues.persistencesiteGateOutputResponse1,
            BusinessPartnerVerboseValues.persistencesiteGateOutputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        val sitesOutput = listOf(
            BusinessPartnerNonVerboseValues.siteGateOutputRequest1,
            BusinessPartnerNonVerboseValues.siteGateOutputRequest2
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val legalEntitiesOutput = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateOutputRequest2,
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        gateClient.legalEntities.upsertLegalEntitiesOutput(legalEntitiesOutput)
        gateClient.sites.upsertSites(sites)
        gateClient.sites.upsertSitesOutput(sitesOutput)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites.getSitesOutput(paginationValue, listOf(BusinessPartnerVerboseValues.externalIdSite1, BusinessPartnerVerboseValues.externalIdSite2))

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*administrativeAreaLevel1*", ".*processStartedAt*").isEqualTo(
            PageDto(
                totalElements = totalElements,
                totalPages = totalPages,
                page = pageValue,
                contentSize = contentSize,
                content = expectedSites
            )
        )
    }
}