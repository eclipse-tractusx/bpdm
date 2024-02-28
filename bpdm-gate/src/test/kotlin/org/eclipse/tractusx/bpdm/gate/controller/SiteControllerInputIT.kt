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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository

import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.gate.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class SiteControllerInputIT @Autowired constructor(
    private val dbHelpers: DbTestHelpers,
    private val assertHelpers: AssertHelpers,
    private val gateClient: GateClient,
    private val siteRepository: SiteRepository
) {
    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

    }

    @BeforeEach
    fun beforeEach() {
        dbHelpers.truncateDbTables()
    }

    /**
     * Given site exists in the database
     * When getting site by external id
     * Then site response should be returned
     */
    @Test
    fun `get site by external id`() {
        val expectedSite = BusinessPartnerVerboseValues.persistencesiteGateInputResponse1

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1
        )

        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        gateClient.sites.upsertSites(sites)

        val site = gateClient.sites.getSiteByExternalId(BusinessPartnerVerboseValues.externalIdSite1)

        assertHelpers.assertRecursively(site).isEqualTo(expectedSite)

    }

    /**
     * Given site does not exist in database
     * When getting site by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get site by external id, not found`() {

        try {
            gateClient.sites.getSiteByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }


    /**
     * Given sites exists
     * When getting sites page
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites`() {
        val expectedSites = listOf(
            BusinessPartnerVerboseValues.persistencesiteGateInputResponse1,
            BusinessPartnerVerboseValues.persistenceSiteGateInputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        gateClient.sites.upsertSites(sites)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites.getSites(paginationValue)

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedSites
        )

        assertHelpers.assertRecursively(pageResponse).isEqualTo(expectedPage)

    }

    /**
     * Given sites exists
     * When getting sites page based on externalId
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites by external id`() {
        val expectedSites = listOf(
            BusinessPartnerVerboseValues.persistencesiteGateInputResponse1,
            BusinessPartnerVerboseValues.persistenceSiteGateInputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val contentSize = 2

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        gateClient.legalEntities.upsertLegalEntities(legalEntities)
        gateClient.sites.upsertSites(sites)

        val externalIds = sites.map { it.externalId }

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites.getSitesByExternalIds(paginationValue, externalIds)

        val expectedPage = PageDto(
            totalElements,
            totalPages,
            page,
            contentSize,
            content = expectedSites
        )

        assertHelpers.assertRecursively(pageResponse).isEqualTo(expectedPage)
    }

    /**
     * When SaaS api responds with an error status code while getting sites
     * Then an internal server error response should be sent
     */
    @Test
    fun `get sites, SaaS error`() {

        val paginationValue = PaginationRequest(0, 10)

        try {
            gateClient.sites.getSites(paginationValue)
        } catch (e: WebClientResponseException) {
            val statusCode: HttpStatusCode = e.statusCode
            val statusCodeValue: Int = statusCode.value()
            assertTrue(statusCodeValue in 500..599)
        }

    }

    /**
     * When requesting too many sites
     * Then a bad request response should be sent
     */
    @Test
    fun `get sites, pagination limit exceeded`() {

        val paginationValue = PaginationRequest(0, 999999)

        try {
            gateClient.sites.getSites(paginationValue)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * Given legal entities in the database
     * When upserting sites of legal entities
     * Then sites entity should be persisted in the database with associated legal entities
     */
    @Test
    fun `upsert sites`() {
        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
            gateClient.sites.upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.OK, e.statusCode)
        }

        //Check if persisted site data
        val siteExternal1 = siteRepository.findByExternalId("site-external-1")
        Assertions.assertNotEquals(siteExternal1, null)

        val siteExternal2 = siteRepository.findByExternalId("site-external-2")
        Assertions.assertNotEquals(siteExternal2, null)

    }

    /**
     * When upserting multiple sites to the database with same externalid
     * Then bad request error should appear
     */
    @Test
    fun `upsert sites, same externalid`() {
        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest1
        )

        val legalEntities = listOf(
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest1,
            BusinessPartnerNonVerboseValues.legalEntityGateInputRequest2
        )

        try {
            gateClient.legalEntities.upsertLegalEntities(legalEntities)
            gateClient.sites.upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
        }

    }

    /**
     * When upserting sites of legal entities using a legal entity external id that does not exist
     * Then a "not_found" response should be sent
     */
    @Test
    fun `upsert sites, legal entity parent not found`() {
        val sites = listOf(
            BusinessPartnerNonVerboseValues.siteGateInputRequest1,
            BusinessPartnerNonVerboseValues.siteGateInputRequest2
        )

        try {
            gateClient.sites.upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }
}