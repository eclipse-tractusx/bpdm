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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PartnerChangelogIT @Autowired constructor(
    val partnerChangelogService: PartnerChangelogService,
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient
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
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2)
            ),
            webTestClient
        )
        val bpnL1 = createdStructures[0].legalEntity.bpn
        val bpnL2 = createdStructures[1].legalEntity.bpn

        // prepare modified partner to import
        val modifiedPartnersToImport = listOf(
            RequestValues.legalEntityUpdate3.copy(bpn = bpnL1)
        )

        // update partner with modified values
        webTestClient.invokePutWithoutResponse(EndpointValues.CATENA_LEGAL_ENTITY_PATH, modifiedPartnersToImport)

        // check changelog entries were created
        // first business partner was created and updated
        retrieveChangelog(bpnL1).content
            .let { changelogEntries ->
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.CREATE }
                    .hasSize(1)
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.UPDATE }
                    .hasSize(1)
            }

        // second business partner was only created
        retrieveChangelog(bpnL2).content
            .let { changelogEntries ->
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.CREATE }
                    .hasSize(1)
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.UPDATE }
                    .isEmpty()
            }
    }

    /**
     * Given some business partners imported
     * When trying to retrieve changelog entries using a nonexistent bpn
     * Then a "not found" response is sent
     */
    @Test
    fun `get changelog entries by nonexistent bpn`() {
        // import partners
        webTestClient.invokePostEndpointWithoutResponse(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)
        )

        val bpn = "NONEXISTENT_BPN"
        webTestClient.get()
            .uri(EndpointValues.CATENA_LEGAL_ENTITY_PATH + "/${bpn}" + EndpointValues.CATENA_CHANGELOG_PATH_POSTFIX)
            .exchange().expectStatus().isNotFound
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

    private fun retrieveChangelog(bpn: String) = webTestClient
        .get()
        .uri(EndpointValues.CATENA_BUSINESS_PARTNERS_PATH + "/${bpn}" + EndpointValues.CATENA_CHANGELOG_PATH_POSTFIX)
        .exchange().expectStatus().isOk
        .returnResult<PageResponse<ChangelogEntryResponse>>()
        .responseBody
        .blockFirst()!!
}