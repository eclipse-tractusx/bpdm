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

package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogSubject
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryResponse
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class],
    properties = ["bpdm.controller.search-request-limit=2"]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class ChangelogControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
    }

    /**
     * Given no legal entities in database
     * When creating and updating legal entities and requesting changelog
     * Then changelog entries for these legal entities
     */
    @Test
    fun `changelog for created and updated legal entities`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2)
            )
        )
        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val bpnA1 = createdStructures[0].legalEntity.legalAddress.bpna
        val bpnA2 = createdStructures[1].legalEntity.legalAddress.bpna


        poolClient.legalEntities().updateBusinessPartners(
            listOf(
                RequestValues.legalEntityUpdate1.copy(bpnl = bpnL1),
                RequestValues.legalEntityUpdate2.copy(bpnl = bpnL2)
            )
        )

        val timeAfterUpdate = Instant.now()

        //Log entry for the created legal entities and their legal addresses should be there
        val expectedChangelogEntries = listOf(
            ChangelogEntryResponse(bpnL1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnL2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnA1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnL1, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnL2, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnA1, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA2, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS)
        )

        val expectedChangelog = PageResponse(expectedChangelogEntries.size.toLong(), 1, 0, expectedChangelogEntries.size, expectedChangelogEntries)

        val actualChangelog = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(),
            PaginationRequest()
        )

        validateChangelogResponse(actualChangelog, expectedChangelog, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given no sites in database
     * When creating and updating sites and requesting changelog
     * Then changelog entries for these sites
     */
    @Test
    fun `changelog for created and updated sites`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = RequestValues.siteCreate1),
                        SiteStructureRequest(site = RequestValues.siteCreate2),
                    )
                )
            )
        )
        val bpnL = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[0].siteStructures[1].site.site.bpns
        val bpnLegalAddress = createdStructures[0].legalEntity.legalAddress.bpna
        val bpnMainAddress1 = createdStructures[0].siteStructures[0].site.mainAddress.bpna
        val bpnMainAddress2 = createdStructures[0].siteStructures[1].site.mainAddress.bpna

        poolClient.sites().updateSite(
            listOf(
                RequestValues.siteUpdate1.copy(bpns = bpnS1),
                RequestValues.siteUpdate2.copy(bpns = bpnS2)
            )
        )

        val timeAfterUpdate = Instant.now()

        //Log entry for the created sites and their main address should be there
        val expectedChangelogEntries = listOf(
            ChangelogEntryResponse(bpnL, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnS1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.SITE),
            ChangelogEntryResponse(bpnS2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.SITE),
            ChangelogEntryResponse(bpnLegalAddress, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnMainAddress1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnMainAddress2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnS1, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.SITE),
            ChangelogEntryResponse(bpnS2, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.SITE),
            ChangelogEntryResponse(bpnMainAddress1, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnMainAddress2, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS)
        )

        val expectedChangelog = PageResponse(expectedChangelogEntries.size.toLong(), 1, 0, expectedChangelogEntries.size, expectedChangelogEntries)

        val actualChangelog = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(),
            PaginationRequest()
        )

        validateChangelogResponse(actualChangelog, expectedChangelog, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given no addresses in database
     * When creating and updating addresses and requesting changelog
     * Then changelog entries for these addresses
     */
    @Test
    fun `changelog for created and updated addresses`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1),
                    siteStructures = listOf(SiteStructureRequest(site = RequestValues.siteCreate1, addresses = listOf(RequestValues.addressPartnerCreate2)))
                )
            )
        )
        val bpnL = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnLegalAddress = createdStructures[0].legalEntity.legalAddress.bpna
        val bpnMainAddress = createdStructures[0].siteStructures[0].site.mainAddress.bpna
        val bpnA1 = createdStructures[0].addresses[0].address.bpna
        val bpnA2 = createdStructures[0].siteStructures[0].addresses[0].address.bpna

        poolClient.addresses().updateAddresses(
            listOf(
                RequestValues.addressPartnerUpdate1.copy(bpna = bpnA1),
                RequestValues.addressPartnerUpdate2.copy(bpna = bpnA2)
            )
        )

        val timeAfterUpdate = Instant.now()

        //Log entry for the created addresses including legal and main address
        val expectedChangelogEntries = listOf(
            ChangelogEntryResponse(bpnL, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnS1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.SITE),
            ChangelogEntryResponse(bpnLegalAddress, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnMainAddress, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA1, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnA2, ChangelogType.UPDATE, timeAfterUpdate, ChangelogSubject.ADDRESS)
        )


        val expectedChangelog = PageResponse(expectedChangelogEntries.size.toLong(), 1, 0, expectedChangelogEntries.size, expectedChangelogEntries)


        val actualChangelog = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(),
            PaginationRequest()
        )

        validateChangelogResponse(actualChangelog, expectedChangelog, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given changelogs in database
     * When paginating through changelogs
     * Then page after page of changelogs returned
     */
    @Test
    fun `paginating changelogs`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3),
            )
        )
        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl
        val bpnL3 = createdStructures[2].legalEntity.legalEntity.bpnl


        val timeAfterUpdate = Instant.now()

        val expectedEntriesFirstPage = listOf(
            ChangelogEntryResponse(bpnL1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnL2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY)
        )

        val expectedEntriesSecondPage = listOf(
            ChangelogEntryResponse(bpnL3, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY)
        )

        val expectedFirstPage = PageResponse(3, 2, 0, expectedEntriesFirstPage.size, expectedEntriesFirstPage)
        val expectedSecondPage = PageResponse(3, 2, 1, expectedEntriesSecondPage.size, expectedEntriesSecondPage)


        val actualFirstPage = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(lsaTypes = setOf(ChangelogSubject.LEGAL_ENTITY)),
            PaginationRequest(page = 0, size = 2)
        )

        val actualSecondPage = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(lsaTypes = setOf(ChangelogSubject.LEGAL_ENTITY)),
            PaginationRequest(page = 1, size = 2)
        )

        validateChangelogResponse(actualFirstPage, expectedFirstPage, timeBeforeInsert, timeAfterUpdate)
        validateChangelogResponse(actualSecondPage, expectedSecondPage, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given changelogs in database
     * When filtering by LSA type
     * Then only get changelog entries of that type
     */
    @Test
    fun `filter by LSA type`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(site = RequestValues.siteCreate1))
                )
            )
        )
        val bpnL = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnS = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnLegalAddress = createdStructures[0].legalEntity.legalAddress.bpna
        val bpnMainAddress = createdStructures[0].siteStructures[0].site.mainAddress.bpna


        val timeAfterUpdate = Instant.now()

        val expectedLegalEntityEntries = listOf(
            ChangelogEntryResponse(bpnL, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY)
        )

        val expectedSiteEntries = listOf(
            ChangelogEntryResponse(bpnS, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.SITE)
        )

        val expectedAddressEntries = listOf(
            ChangelogEntryResponse(bpnLegalAddress, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS),
            ChangelogEntryResponse(bpnMainAddress, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.ADDRESS)
        )

        val expectedLegalEntitiesPage =
            PageResponse(expectedLegalEntityEntries.size.toLong(), 1, 0, expectedLegalEntityEntries.size, expectedLegalEntityEntries)
        val expectedSitesPage = PageResponse(expectedSiteEntries.size.toLong(), 1, 0, expectedSiteEntries.size, expectedSiteEntries)
        val expectedAddressesPage = PageResponse(expectedAddressEntries.size.toLong(), 1, 0, expectedAddressEntries.size, expectedAddressEntries)


        val actualLegalEntityPage = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(lsaTypes = setOf(ChangelogSubject.LEGAL_ENTITY)),
            PaginationRequest()
        )
        val actualSitePage = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(lsaTypes = setOf(ChangelogSubject.SITE)),
            PaginationRequest()
        )
        val actualAddressPage = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(lsaTypes = setOf(ChangelogSubject.ADDRESS)),
            PaginationRequest()
        )

        validateChangelogResponse(actualLegalEntityPage, expectedLegalEntitiesPage, timeBeforeInsert, timeAfterUpdate)
        validateChangelogResponse(actualSitePage, expectedSitesPage, timeBeforeInsert, timeAfterUpdate)
        validateChangelogResponse(actualAddressPage, expectedAddressesPage, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given changelogs
     * When filtering changelogs by BPN
     * Then only get changelogs with these BPNs
     */
    @Test
    fun `filter by BPNs`() {

        val timeBeforeInsert = Instant.now()

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3)
            )
        )
        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl


        val timeAfterUpdate = Instant.now()

        //Log entry for the created legal entities and their legal addresses should be there
        val expectedChangelogEntries = listOf(
            ChangelogEntryResponse(bpnL1, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnL2, ChangelogType.CREATE, timeAfterUpdate, ChangelogSubject.LEGAL_ENTITY)
        )

        val expectedChangelog = PageResponse(expectedChangelogEntries.size.toLong(), 1, 0, expectedChangelogEntries.size, expectedChangelogEntries)

        val actualChangelog = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(bpns = setOf(bpnL1, bpnL2)),
            PaginationRequest()
        )

        validateChangelogResponse(actualChangelog, expectedChangelog, timeBeforeInsert, timeAfterUpdate)
    }

    /**
     * Given changelogs
     * When filtering changelogs by timestamp
     * Then only get changelogs having timestamps the given time
     */
    @Test
    fun `filter by timestamp`() {

        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate1)
            )
        )

        val timeAfterFirstInsert = Instant.now()


        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate2),
                LegalEntityStructureRequest(legalEntity = RequestValues.legalEntityCreate3)
            )
        )

        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val timeAfterSecondInsert = Instant.now()

        //Log entry for the created legal entities and their legal addresses should be there
        val expectedChangelogEntries = listOf(
            ChangelogEntryResponse(bpnL1, ChangelogType.CREATE, timeAfterSecondInsert, ChangelogSubject.LEGAL_ENTITY),
            ChangelogEntryResponse(bpnL2, ChangelogType.CREATE, timeAfterSecondInsert, ChangelogSubject.LEGAL_ENTITY)
        )

        val expectedChangelog = PageResponse(expectedChangelogEntries.size.toLong(), 1, 0, expectedChangelogEntries.size, expectedChangelogEntries)

        val actualChangelog = poolClient.changelogs().getChangelogEntries(
            ChangelogSearchRequest(fromTime = timeAfterFirstInsert, lsaTypes = setOf(ChangelogSubject.LEGAL_ENTITY)),
            PaginationRequest()
        )

        validateChangelogResponse(actualChangelog, expectedChangelog, timeAfterFirstInsert, timeAfterSecondInsert)
    }


    private fun checkTimestampAscending() =
        { changelogEntries: Collection<ChangelogEntryResponse> ->
            var lastTimestamp = Instant.MIN
            for (changelogEntry in changelogEntries) {
                Assertions.assertThat(changelogEntry.timestamp).isAfterOrEqualTo(lastTimestamp)
                lastTimestamp = changelogEntry.timestamp
            }
        }

    private fun checkTimestampsInBetween(after: Instant, before: Instant) =
        { changelogEntries: Collection<ChangelogEntryResponse> ->
            changelogEntries.forEach { entry ->
                Assertions.assertThat(entry.timestamp).isBetween(after, before)
            }
        }

    private fun checkEqual(expected: PageResponse<ChangelogEntryResponse>) =
        { actualResponse: PageResponse<ChangelogEntryResponse> ->
            testHelpers.assertRecursively(actualResponse).isEqualTo(expected)
            Unit
        }

    private fun validateChangelogResponse(
        actual: PageResponse<ChangelogEntryResponse>,
        expected: PageResponse<ChangelogEntryResponse>,
        timeBeforeInsert: Instant,
        timeAfterInsert: Instant
    ) {
        actual.also(checkEqual(expected))
            .content
            .also(checkTimestampAscending())
            .also(checkTimestampsInBetween(timeBeforeInsert, timeAfterInsert))
    }


}