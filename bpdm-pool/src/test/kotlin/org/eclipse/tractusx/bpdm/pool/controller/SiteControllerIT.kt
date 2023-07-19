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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SiteControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolClientImpl
) {
    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
    }

    /**
     * Given partners in db
     * When requesting a site by bpn-s
     * Then site is returned
     */
    @Test
    fun `get site by bpn-s`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1))

        val importedPartner = createdStructures.single().legalEntity
        importedPartner.legalEntity.bpnl
            .let { bpn -> requestSitesOfLegalEntity(bpn).content.single().bpns }
            .let { bpnSite -> requestSite(bpnSite) }
            .let { siteResponse ->
                assertThat(siteResponse.site.bpnLegalEntity).isEqualTo(importedPartner.legalEntity.bpnl)
            }
    }

    /**
     * Given partners in db
     * When requesting an site by non-existent bpn-s
     * Then a "not found" response is sent
     */
    @Test
    fun `get site by bpn-s, not found`() {
        testHelpers.createBusinessPartnerStructure(listOf(RequestValues.partnerStructure1))
        testHelpers.`get site by bpn-s, not found`("NONEXISTENT_BPN")
    }

    /**
     * Given sites
     * When searching for sites via BPNS
     * Then return those sites
     */
    @Test
    fun `search sites by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = RequestValues.siteCreate1),
                        SiteStructureRequest(site = RequestValues.siteCreate2),
                        SiteStructureRequest(site = RequestValues.siteCreate3)
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[0].siteStructures[1].site.site.bpns
        val bpnL = createdStructures[0].legalEntity.legalEntity.bpnl

        val siteSearchRequest = SiteBpnSearchRequest(emptyList(), listOf(bpnS1, bpnS2))
        val searchResult = poolClient.sites().searchSites(siteSearchRequest, PaginationRequest())

        val expectedSiteWithReference1 = SitePoolVerboseDto(
            site = ResponseValues.site1.copy(bpnLegalEntity = bpnL),
            mainAddress = ResponseValues.addressPartner1.copy(isMainAddress = true, bpnSite = CommonValues.bpnS1)
        )
        val expectedSiteWithReference2 = SitePoolVerboseDto(
            site = ResponseValues.site2.copy(bpnLegalEntity = bpnL),
            mainAddress = ResponseValues.addressPartner2.copy(isMainAddress = true, bpnSite = CommonValues.bpnS2)
        )

        testHelpers.assertRecursively(searchResult.content)
            .ignoringFieldsOfTypes(Instant::class.java)
            .ignoringFields(
                SitePoolVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
            )
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2))
    }

    /**
     * Given sites of business partners
     * When searching for sites via BPNL
     * Then return sites that belong to those legal entities
     */
    @Test
    fun `search sites by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = RequestValues.siteCreate1),
                        SiteStructureRequest(site = RequestValues.siteCreate2),
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(site = RequestValues.siteCreate3))
                )
            )
        )

        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val siteSearchRequest = SiteBpnSearchRequest(listOf(bpnL1, bpnL2))
        val searchResult = poolClient.sites().searchSites(siteSearchRequest, PaginationRequest())

        val expectedSiteWithReference1 =
            SitePoolVerboseDto(
                site = ResponseValues.site1.copy(bpnLegalEntity = bpnL1),
                mainAddress = ResponseValues.addressPartner1.copy(isMainAddress = true, bpnSite = CommonValues.bpnS1)
            )
        val expectedSiteWithReference2 =
            SitePoolVerboseDto(
                site = ResponseValues.site2.copy(bpnLegalEntity = bpnL1),
                mainAddress = ResponseValues.addressPartner2.copy(isMainAddress = true, bpnSite = CommonValues.bpnS2)
            )
        val expectedSiteWithReference3 =
            SitePoolVerboseDto(
                site = ResponseValues.site3.copy(bpnLegalEntity = bpnL2),
                mainAddress = ResponseValues.addressPartner3.copy(isMainAddress = true, bpnSite = CommonValues.bpnS3)
            )

        testHelpers.assertRecursively(searchResult.content)
            .ignoringFieldsOfTypes(Instant::class.java)
            .ignoringFields(
                SitePoolVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
            )
            .isEqualTo(listOf(expectedSiteWithReference1, expectedSiteWithReference2, expectedSiteWithReference3))
    }


    /**
     * Given legal entities
     * When requesting new sites for legal entities
     * Then new sites with BPN returned
     */
    @Test
    fun `create new sites`() {

        val givenLegalEntities =
            poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().legalEntity.bpnl
        val bpnL2 = givenLegalEntities.last().legalEntity.bpnl

        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2, ResponseValues.siteUpsert3)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(bpnlParent = bpnL1),
            RequestValues.siteCreate2.copy(bpnlParent = bpnL2),
            RequestValues.siteCreate3.copy(bpnlParent = bpnL2)
        )

        val response = poolClient.sites().createSite(toCreate)

        assertThatCreatedSitesEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given legal entities
     * When creating sites with existing and non-existing bpnl
     * Then only create sites with existing bpnl
     */
    @Test
    fun `don't create sites with non-existing parent`() {


        val givenLegalEntities =
            poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1, RequestValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().legalEntity.bpnl
        val bpnL2 = givenLegalEntities.last().legalEntity.bpnl


        val expected = listOf(ResponseValues.siteUpsert1, ResponseValues.siteUpsert2)

        val toCreate = listOf(
            RequestValues.siteCreate1.copy(bpnlParent = bpnL1),
            RequestValues.siteCreate2.copy(bpnlParent = bpnL2),
            RequestValues.siteCreate3.copy(bpnlParent = "NONEXISTENT")
        )
        val response = poolClient.sites().createSite(toCreate)

        // 2 entities okay
        assertThatCreatedSitesEqual(response.entities, expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteCreateError.LegalEntityNotFound, CommonValues.index3)
    }

    /**
     * Given sites
     * When updating sites via BPN
     * Then update those sites
     */
    @Test
    fun `update existing sites`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.site.bpns
        val bpnS2 = givenStructure[1].siteStructures[0].site.site.bpns
        val bpnS3 = givenStructure[1].siteStructures[1].site.site.bpns

        val expected = listOf(
            ResponseValues.siteUpsert1.run { copy(site = site.copy(bpns = bpnS3), index = null) },
            ResponseValues.siteUpsert2.run { copy(site = site.copy(bpns = bpnS1), index = null) },
            ResponseValues.siteUpsert3.run { copy(site = site.copy(bpns = bpnS2), index = null) },
        )

        val toUpdate = listOf(
            RequestValues.siteUpdate1.copy(bpns = bpnS3),
            RequestValues.siteUpdate2.copy(bpns = bpnS1),
            RequestValues.siteUpdate3.copy(bpns = bpnS2)
        )

        val response = poolClient.sites().updateSite(toUpdate)

        assertThatCreatedSitesEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given sites
     * When updating via existent and non-existent BPNs
     * Then only update sites with existent BPN
     */
    @Test
    fun `ignore non-existent BPNS updates`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1), SiteStructureRequest(RequestValues.siteCreate2))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.site.bpns
        val bpnS2 = givenStructure[0].siteStructures[1].site.site.bpns

        val expected = listOf(
            ResponseValues.siteUpsert1.run { copy(site = site.copy(bpns = bpnS2), index = null) },
            ResponseValues.siteUpsert2.run { copy(site = site.copy(bpns = bpnS1), index = null) },
        )

        val toUpdate = listOf(
            RequestValues.siteUpdate1.copy(bpns = bpnS2),
            RequestValues.siteUpdate2.copy(bpns = bpnS1),
            RequestValues.siteUpdate3.copy(bpns = "NONEXISTENT"),
        )
        val response = poolClient.sites().updateSite(toUpdate)

        // 2 entities okay
        assertThatCreatedSitesEqual(response.entities, expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteUpdateError.SiteNotFound, "NONEXISTENT")
    }

    /**
     * Given sites
     * When asking for main addresses by site BPNs
     * Then main addresses of sites returned
     */
    @Test
    fun `find main addresses by BPNS`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { it.site.mainAddress }

        val toSearch = expected.map { it.bpnSite!! }

        val response = poolClient.sites().searchMainAddresses(toSearch)
        testHelpers.assertRecursively(response).isEqualTo(expected)
    }

    /**
     * Given sites
     * When asking for main addresses with non-existent BPNs
     * Then only main addresses of sites with existing BPNs returned
     */
    @Test
    fun `find main address, ignore invalid BPNS`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate2), SiteStructureRequest(RequestValues.siteCreate3))
                )
            )
        )

        val expected = givenStructure.flatMap { it.siteStructures }.map { it.site.mainAddress }

        val toSearch = expected.map { it.bpnSite!! }.plus("NON-EXISTENT")

        val response = poolClient.sites().searchMainAddresses(toSearch)
        testHelpers.assertRecursively(response).isEqualTo(expected)
    }

    @Test
    fun `retrieve sites with pagination`() {

        lateinit var site1: SiteVerboseDto

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(site = RequestValues.siteCreate1))
                )
            )
        )

        val legalAddress1: LogisticAddressVerboseDto =
            ResponseValues.addressPartner1.copy(isMainAddress = true, bpnSite = CommonValues.bpnS1, bpna = "BPNA0000000001YN")
        site1 = givenStructure[0].siteStructures[0].site.site

        val expectedFirstPage = PageDto(
            1, 1, 0, 1, listOf(
                SiteMatchVerboseDto(mainAddress = legalAddress1, site = site1)
            )
        )

        val firstPage = poolClient.sites().getSitesPaginated(paginationRequest = PaginationRequest(0, 10))

        testHelpers.assertRecursively(firstPage).ignoringFieldsOfTypes(Instant::class.java).isEqualTo(expectedFirstPage)

    }

    private fun assertThatCreatedSitesEqual(actuals: Collection<SitePartnerCreateVerboseDto>, expected: Collection<SitePartnerCreateVerboseDto>) {
        actuals.forEach { assertThat(it.site.bpns).matches(testHelpers.bpnSPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields(
                SitePartnerCreateVerboseDto::site.name + "." + SiteVerboseDto::bpns.name,
                SitePartnerCreateVerboseDto::site.name + "." + SiteVerboseDto::bpnLegalEntity.name,
                SitePartnerCreateVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
                SitePartnerCreateVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpnSite.name
            )
            .isEqualTo(expected)
    }

    private fun requestSite(bpnSite: String) = poolClient.sites().getSite(bpnSite)

    private fun requestSitesOfLegalEntity(bpn: String) = poolClient.legalEntities().getSites(bpn, PaginationRequest())

}