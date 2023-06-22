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
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.eclipse.tractusx.bpdm.gate.util.*
import org.eclipse.tractusx.bpdm.gate.util.EndpointValues.SAAS_MOCK_BUSINESS_PARTNER_PATH
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class SiteControllerInputIT @Autowired constructor(
    val gateClient: GateClient,
    private val siteRepository: SiteRepository,
    private val dbTestHelpers: DbTestHelpers
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

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
    }

    /**
     * Given site exists in the database
     * When getting site by external id
     * Then site response should be returned
     */
    @Test
    fun `get site by external id`() {
        val expectedSite = ResponseValues.persistencesiteGateInputResponse1

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1
        )

        val sites = listOf(
            RequestValues.siteGateInputRequest1
        )

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        gateClient.sites().upsertSites(sites)

        val site = gateClient.sites().getSiteByExternalId(CommonValues.externalIdSite1)

        assertThat(site).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*administrativeAreaLevel1*").isEqualTo(expectedSite)
    }

    /**
     * Given site does not exist in database
     * When getting site by external id
     * Then "not found" response is sent
     */
    @Test
    fun `get site by external id, not found`() {

        try {
            gateClient.sites().getSiteByExternalId("nonexistent-externalid123")
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }

//    /**
//     * When SaaS api responds with an error status code while fetching site by external id
//     * Then an internal server error response should be sent
//     */
//    @Test
//    fun `DEPRECATED get site by external id, SaaS error`() {
//        wireMockServer.stubFor(
//            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
//                .willReturn(badRequest())
//        )
//
//        try {
//            gateClient.sites().getSiteByExternalId(SaasValues.legalEntityRequest1.externalId.toString())
//        } catch (e: WebClientResponseException) {
//            val statusCode: HttpStatusCode = e.statusCode
//            val statusCodeValue: Int = statusCode.value()
//            assertTrue(statusCodeValue in 500..599)
//        }
//    }

//    /**
//     * Given site without main address in SaaS
//     * When query by its external ID
//     * Then server error is returned
//     */
//    @Test
//    fun `DEPRECATED get site without main address, expect error`() {
//
//        val invalidPartner = SaasValues.siteBusinessPartnerWithRelations1.copy(addresses = emptyList())
//
//        wireMockServer.stubFor(
//            post(urlPathMatching(EndpointValues.SAAS_MOCK_FETCH_BUSINESS_PARTNER_PATH))
//                .willReturn(
//                    aResponse()
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(
//                            objectMapper.writeValueAsString(
//                                FetchResponse(
//                                    businessPartner = invalidPartner,
//                                    status = FetchResponse.Status.OK
//                                )
//                            )
//                        )
//                )
//        )
//
//        try {
//            gateClient.sites().getSiteByExternalId(SaasValues.siteBusinessPartnerWithRelations1.externalId.toString())
//        } catch (e: WebClientResponseException) {
//            val statusCode: HttpStatusCode = e.statusCode
//            val statusCodeValue: Int = statusCode.value()
//            assertTrue(statusCodeValue in 500..599)
//        }
//
//    }

    /**
     * Given sites exists in SaaS
     * When getting sites page
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites`() {
        val expectedSites = listOf(
            ResponseValues.persistencesiteGateInputResponse1,
            ResponseValues.persistenceSiteGateInputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        val sites = listOf(
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        gateClient.sites().upsertSites(sites)

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites().getSites(paginationValue)

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*administrativeAreaLevel1*").isEqualTo(
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
     * Given sites exists in SaaS
     * When getting sites page based on externalId
     * Then sites page mapped to the catena data model should be returned
     */
    @Test
    fun `get sites by external id`() {
        val expectedSites = listOf(
            ResponseValues.persistencesiteGateInputResponse1,
            ResponseValues.persistenceSiteGateInputResponse2
        )

        val page = 0
        val size = 10

        val totalElements = 2L
        val totalPages = 1
        val pageValue = 0
        val contentSize = 2

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        val sites = listOf(
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )

        gateClient.legalEntities().upsertLegalEntities(legalEntities)
        gateClient.sites().upsertSites(sites)

        val externalIds = sites.map { it.externalId }

        val paginationValue = PaginationRequest(page, size)
        val pageResponse = gateClient.sites().getSitesByExternalIds(paginationValue, externalIds)

        assertThat(pageResponse).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*administrativeAreaLevel1*").isEqualTo(
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
     * When SaaS api responds with an error status code while getting sites
     * Then an internal server error response should be sent
     */
    @Test
    fun `get sites, SaaS error`() {
        wireMockServer.stubFor(
            get(urlPathMatching(SAAS_MOCK_BUSINESS_PARTNER_PATH))
                .willReturn(badRequest())
        )

        val paginationValue = PaginationRequest(0, 10)

        try {
            gateClient.sites().getSites(paginationValue)
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
            gateClient.sites().getSites(paginationValue)
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
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
            gateClient.sites().upsertSites(sites)
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
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest1
        )

        val legalEntities = listOf(
            RequestValues.legalEntityGateInputRequest1,
            RequestValues.legalEntityGateInputRequest2
        )

        try {
            gateClient.legalEntities().upsertLegalEntities(legalEntities)
            gateClient.sites().upsertSites(sites)
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
            RequestValues.siteGateInputRequest1,
            RequestValues.siteGateInputRequest2
        )

        try {
            gateClient.sites().upsertSites(sites)
        } catch (e: WebClientResponseException) {
            assertEquals(HttpStatus.NOT_FOUND, e.statusCode)
        }

    }
}