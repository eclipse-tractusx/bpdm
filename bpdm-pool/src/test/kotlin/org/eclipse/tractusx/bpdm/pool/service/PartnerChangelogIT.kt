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

package org.eclipse.tractusx.bpdm.pool.service

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.config.ControllerConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryDto
import org.eclipse.tractusx.bpdm.pool.dto.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.entity.ChangelogSubject
import org.eclipse.tractusx.bpdm.pool.entity.ChangelogType
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.time.Instant
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class],
    properties = ["bpdm.controller.search-request-limit=2"])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PartnerChangelogIT @Autowired constructor(
    val partnerChangelogService: PartnerChangelogService,
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient,
    val controllerConfigProperties: ControllerConfigProperties,
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata(webTestClient)
    }

    /**
     * Given no partners in db
     * When importing partners and then updating partners
     * Then create and update changelog entries are created
     */
    @Test
    fun `create changelog entries`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3)
            ),
            webTestClient
        )
        val bpnL1 = createdStructures[0].legalEntity.bpn
        val bpnL2 = createdStructures[1].legalEntity.bpn

        // prepare modified partner to import
        val modifiedPartnersToImport = listOf(
            RequestValues.legalEntityUpdate3.copy(bpn = bpnL1)
        )

        val timestampBetweenCreateAndUpdate = Instant.now()

        // update partner with modified values
        webTestClient.invokePutWithoutResponse(EndpointValues.CATENA_LEGAL_ENTITY_PATH, modifiedPartnersToImport)


        // no restrictions for BPNs and timestamp
        retrieveChangelog(null, null).content
            .also(checkNumberCreatedAndUpdated(3, 1))
            .also(checkTimestampAscending())

        // no restrictions for timestamp, but limited to multiple BPNs
        retrieveChangelog(null, listOf(bpnL1, bpnL2)).content
            .let(checkNumberCreatedAndUpdated(2, 1))

        // check changelog entries were created
        // first business partner was created and updated
        retrieveChangelog(null, listOf(bpnL1)).content
            .let(checkNumberCreatedAndUpdated(1, 1))

        // second business partner was only created
        retrieveChangelog(null, listOf(bpnL2)).content
            .let(checkNumberCreatedAndUpdated(1, 0))

        // filter out CREATED changelogs, only return one UPDATED changelog for BPNL1
        retrieveChangelog(timestampBetweenCreateAndUpdate, null).content
            .let(checkNumberCreatedAndUpdated(0, 1))

        // filter out all changelogs
        retrieveChangelog(Instant.now(), null).content
            .let(checkNumberCreatedAndUpdated(0, 0))

        // filter out CREATED changelogs, restrict to BPNL1
        retrieveChangelog(timestampBetweenCreateAndUpdate, listOf(bpnL1)).content
            .let(checkNumberCreatedAndUpdated(0, 1))

        // filter out CREATED changelogs, restrict to BPNL2, resulting in no entries
        retrieveChangelog(timestampBetweenCreateAndUpdate, listOf(bpnL2)).content
            .let(checkNumberCreatedAndUpdated(0, 0))
    }

    /**
     * Given some business partners imported
     * When trying to retrieve changelog entries using a nonexistent bpn
     * Then an empty response is sent
     */
    @Test
    fun `get changelog entries by nonexistent bpn`() {
        // import partners
        webTestClient.invokePostEndpointWithoutResponse(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        val bpn = "NONEXISTENT_BPN"
        assertThat(retrieveChangelog(null, listOf(bpn)).contentSize).isEqualTo(0)
    }

    /**
     * Given some business partners imported
     * When trying to retrieve changelog entries using an invalid timestamp
     * Then a "bad request" response is sent
     */
    @Test
    fun `get changelog entries by invalid timestamp`() {
        // import partners
        webTestClient.invokePostEndpointWithoutResponse(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        getChangelogResponse("NO_VALID_TIME", null)
            .expectStatus().isBadRequest

        getChangelogResponse("2023-15-99T08:22:54.986733Z", null)
            .expectStatus().isBadRequest
    }

    /**
     * Given some business partners imported
     * When trying to retrieve changelog entries and using too many BPNs in the request
     * Then a "bad request" response is sent
     */
    @Test
    fun `get changelog entries using too many search params`() {
        // import partners
        webTestClient.invokePostEndpointWithoutResponse(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        val limit = controllerConfigProperties.searchRequestLimit
        val bpnsOk = (0..limit-1).map { it -> "bpn${it}" }      // limit entries
        getChangelogResponse(null, bpnsOk)
            .expectStatus().isOk

        val bpnsTooMany = (0..limit).map { it -> "bpn${it}" }      // limit+1 entries
        getChangelogResponse(null, bpnsTooMany)
            .expectStatus().isBadRequest
    }

    /**
     * Given some changelog entries in db
     * When changelog entries retrieved paginated
     * Then correct changelog page retrieved
     */
    @Test
    fun `get changelog entries starting after id via service`() {
        val startId = partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.CREATE, ChangelogSubject.LEGAL_ENTITY)).id
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.UPDATE, ChangelogSubject.LEGAL_ENTITY))
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn2", ChangelogType.CREATE, ChangelogSubject.LEGAL_ENTITY))

        val changelogEntryPage = partnerChangelogService.getChangelogEntriesStartingAfterId(startId = startId, pageIndex = 1, pageSize = 1)
        assertThat(changelogEntryPage.totalElements).isEqualTo(2)
        assertThat(changelogEntryPage.content.size).isEqualTo(1)
        assertThat(changelogEntryPage.content[0].bpn).isEqualTo("testBpn2")
    }

    private fun checkNumberCreatedAndUpdated(created: Int, updated: Int) =
        { changelogEntries: Collection<ChangelogEntryResponse> ->
            assertThat(changelogEntries)
                .filteredOn { it.changelogType == ChangelogType.CREATE }
                .hasSize(created)
            assertThat(changelogEntries)
                .filteredOn { it.changelogType == ChangelogType.UPDATE }
                .hasSize(updated)
            Unit
        }

    private fun checkTimestampAscending() =
        { changelogEntries: Collection<ChangelogEntryResponse> ->
            var lastTimestamp = Instant.MIN
            for (changelogEntry in changelogEntries) {
                assertThat(changelogEntry.timestamp).isAfterOrEqualTo(lastTimestamp)
                lastTimestamp = changelogEntry.timestamp
            }
        }

    private fun retrieveChangelog(modifiedAfter: Instant?, bpn: Collection<String>?): PageResponse<ChangelogEntryResponse> =
        getChangelogResponse(modifiedAfter, bpn)
            .expectStatus().isOk
            .returnResult<PageResponse<ChangelogEntryResponse>>()
            .responseBody
            .blockFirst()!!

    private fun getChangelogResponse(modifiedAfter: Any?, bpn: Collection<String>?) =
        webTestClient
            .get()
            .uri {
                it.path(EndpointValues.CATENA_BUSINESS_PARTNERS_PATH + EndpointValues.CATENA_CHANGELOG_PATH_POSTFIX)
                    .queryParamIfPresent("modifiedAfter", Optional.ofNullable(modifiedAfter))
                    .queryParamIfPresent("bpn", Optional.ofNullable(bpn))
                    .build()
            }
            .exchange()
}