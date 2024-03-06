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

package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.util.TestHelpers
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.LegalEntityStructureRequest
import org.eclipse.tractusx.bpdm.test.testdata.pool.SiteStructureRequest
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
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
    val poolClient: PoolClientImpl,
    val dbTestHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
    val poolDataHelpers: PoolDataHelpers,
) {
    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()
    }

    /**
     * Given partners in db
     * When requesting a site by bpn-s
     * Then site is returned
     */
    @Test
    fun `get site by bpn-s`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(listOf(BusinessPartnerNonVerboseValues.partnerStructure1))

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
        testHelpers.createBusinessPartnerStructure(listOf(BusinessPartnerNonVerboseValues.partnerStructure1))
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
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate1),
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate2),
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate3)
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[0].siteStructures[1].site.site.bpns
        val bpnL = createdStructures[0].legalEntity.legalEntity.bpnl

        val siteSearchRequest = SiteBpnSearchRequest(emptyList(), listOf(bpnS1, bpnS2))
        val searchResult = poolClient.sites.searchSites(siteSearchRequest, PaginationRequest())

        val expectedSiteWithReference1 = SiteWithMainAddressVerboseDto(
            site = BusinessPartnerVerboseValues.site1.copy(bpnLegalEntity = bpnL),
            mainAddress = BusinessPartnerVerboseValues.addressPartner1.copy(
                isMainAddress = true,
                addressType = AddressType.SiteMainAddress,
                bpnSite = BusinessPartnerVerboseValues.site1.bpns
            )
        )
        val expectedSiteWithReference2 = SiteWithMainAddressVerboseDto(
            site = BusinessPartnerVerboseValues.site2.copy(bpnLegalEntity = bpnL),
            mainAddress = BusinessPartnerVerboseValues.addressPartner2.copy(
                isMainAddress = true,
                addressType = AddressType.SiteMainAddress,
                bpnSite = BusinessPartnerVerboseValues.site2.bpns
            )
        )

        assertHelpers.assertRecursively(searchResult.content)
            .ignoringFieldsOfTypes(Instant::class.java)
            .ignoringFields(
                SiteWithMainAddressVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
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
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate1),
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate2),
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate3))
                )
            )
        )

        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl
        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val siteSearchRequest = SiteBpnSearchRequest(listOf(bpnL1, bpnL2))
        val searchResult = poolClient.sites.searchSites(siteSearchRequest, PaginationRequest())

        val expectedSiteWithReference1 =
            SiteWithMainAddressVerboseDto(
                site = BusinessPartnerVerboseValues.site1.copy(bpnLegalEntity = bpnL1),
                mainAddress = BusinessPartnerVerboseValues.addressPartner1.copy(
                    isMainAddress = true,
                    addressType = AddressType.SiteMainAddress,
                    bpnSite = BusinessPartnerVerboseValues.site1.bpns
                )
            )
        val expectedSiteWithReference2 =
            SiteWithMainAddressVerboseDto(
                site = BusinessPartnerVerboseValues.site2.copy(bpnLegalEntity = bpnL1),
                mainAddress = BusinessPartnerVerboseValues.addressPartner2.copy(
                    isMainAddress = true,
                    addressType = AddressType.SiteMainAddress,
                    bpnSite = BusinessPartnerVerboseValues.site2.bpns
                )
            )
        val expectedSiteWithReference3 =
            SiteWithMainAddressVerboseDto(
                site = BusinessPartnerVerboseValues.site3.copy(bpnLegalEntity = bpnL2),
                mainAddress = BusinessPartnerVerboseValues.addressPartner3.copy(
                    isMainAddress = true,
                    addressType = AddressType.SiteMainAddress,
                    bpnSite = BusinessPartnerVerboseValues.site3.bpns
                )
            )

        assertHelpers.assertRecursively(searchResult.content)
            .ignoringFieldsOfTypes(Instant::class.java)
            .ignoringFields(
                SiteWithMainAddressVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
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
            poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1, BusinessPartnerNonVerboseValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().legalEntity.bpnl
        val bpnL2 = givenLegalEntities.last().legalEntity.bpnl

        val expected = listOf(BusinessPartnerVerboseValues.siteUpsert1, BusinessPartnerVerboseValues.siteUpsert2, BusinessPartnerVerboseValues.siteUpsert3)

        val toCreate = listOf(
            BusinessPartnerNonVerboseValues.siteCreate1.copy(bpnlParent = bpnL1),
            BusinessPartnerNonVerboseValues.siteCreate2.copy(bpnlParent = bpnL2),
            BusinessPartnerNonVerboseValues.siteCreate3.copy(bpnlParent = bpnL2)
        )

        val response = poolClient.sites.createSite(toCreate)

        assertThatCreatedSitesEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }


    /**
     * Given no legal entities
     * When creating some sites entities in one request that have duplicate identifiers on the address (regarding type and value)
     * Then for these sites entities an error is returned
     */
    @Test
    fun `create sites entities and get duplicate identifier error on address`() {
        poolClient.metadata.createIdentifierType(
            IdentifierTypeDto(
                technicalKey = BusinessPartnerNonVerboseValues.addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = BusinessPartnerNonVerboseValues.addressIdentifier.value
            )
        )

        val givenLegalEntities =
            poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1, BusinessPartnerNonVerboseValues.legalEntityCreate2)).entities

        val request1 = with(BusinessPartnerNonVerboseValues.siteCreate1) {
            copy(
                index = BusinessPartnerNonVerboseValues.siteCreate1.index,
                site = site.copy(
                    name = BusinessPartnerNonVerboseValues.siteCreate1.site.name,
                    states = listOf(BusinessPartnerNonVerboseValues.siteStatus1),
                    mainAddress = BusinessPartnerNonVerboseValues.logisticAddress3.copy(
                        identifiers = listOf(BusinessPartnerNonVerboseValues.addressIdentifier)
                    )
                )
            )
        }
        val request2 = with(BusinessPartnerNonVerboseValues.siteCreate2) {
            copy(
                index = BusinessPartnerNonVerboseValues.siteCreate1.index,
                site = site.copy(
                    name = BusinessPartnerNonVerboseValues.siteCreate1.site.name,
                    states = listOf(BusinessPartnerNonVerboseValues.siteStatus1),
                    mainAddress = BusinessPartnerNonVerboseValues.logisticAddress2.copy(
                        identifiers = listOf(BusinessPartnerNonVerboseValues.addressIdentifier)
                    )
                )
            )
        }

        val response = poolClient.sites.createSite(
            listOf(request1, request2)
        )

        assertThat(response.errorCount).isEqualTo(2)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], SiteCreateError.MainAddressDuplicateIdentifier, request1.index!!)
        testHelpers.assertErrorResponse(errors[1], SiteCreateError.MainAddressDuplicateIdentifier, request2.index!!)

    }

    /**
     * Given no legal entities
     * When creating some site entities in one request that have duplicate identifiers (regarding type and value)
     * Then for these site entities an error is returned
     */
    @Test
    fun `update site entities and get duplicate identifier error`() {

        poolClient.metadata.createIdentifierType(
            IdentifierTypeDto(
                technicalKey = BusinessPartnerNonVerboseValues.addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = BusinessPartnerNonVerboseValues.addressIdentifier.value
            )
        )

        val givenLegalEntities =
            poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1, BusinessPartnerNonVerboseValues.legalEntityCreate2)).entities

        val toCreate1 = listOf(BusinessPartnerNonVerboseValues.siteCreate1, BusinessPartnerNonVerboseValues.siteCreate2)
        val response1 = poolClient.sites.createSite(toCreate1)


        assertThat(response1.errorCount).isEqualTo(0)
        val bpnList = response1.entities.map { it.site.bpns }

        // 2 equivalent identifiers (in regard to fields type and value) but different from the identifiers in the DB
        val referenceIdentifier = BusinessPartnerNonVerboseValues.identifier3.copy(
            issuingBody = BusinessPartnerNonVerboseValues.identifier1.issuingBody
        )
        val equivalentIdentifier = referenceIdentifier.copy(
            issuingBody = BusinessPartnerNonVerboseValues.identifier2.issuingBody
        )

        // 3 requests using these equivalent identifiers & 1 different request
        val toUpdate1 = with(BusinessPartnerNonVerboseValues.siteUpdate1) {
            copy(
                bpns = bpnList[0],
                site = site.copy(
                    name = BusinessPartnerNonVerboseValues.siteUpdate1.site.name,
                    states = listOf(BusinessPartnerNonVerboseValues.siteStatus1),
                    mainAddress = BusinessPartnerNonVerboseValues.logisticAddress3.copy(
                        identifiers = listOf(BusinessPartnerNonVerboseValues.addressIdentifier)
                    )
                )
            )
        }
        val toUpdate2 = with(BusinessPartnerNonVerboseValues.siteUpdate2) {
            copy(
                bpns = bpnList[1],
                site = site.copy(
                    name = BusinessPartnerNonVerboseValues.siteUpdate1.site.name,
                    states = listOf(BusinessPartnerNonVerboseValues.siteStatus1),
                    mainAddress = BusinessPartnerNonVerboseValues.logisticAddress2.copy(
                        identifiers = listOf(BusinessPartnerNonVerboseValues.addressIdentifier)
                    )
                )
            )
        }

        val response = poolClient.sites.updateSite(
            listOf(toUpdate1, toUpdate2)
        )

        assertThat(response.errorCount).isEqualTo(2)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], SiteUpdateError.MainAddressDuplicateIdentifier, toUpdate1.bpns)
        testHelpers.assertErrorResponse(errors[1], SiteUpdateError.MainAddressDuplicateIdentifier, toUpdate2.bpns)
    }

    /**
     * Given legal entities
     * When creating sites with existing and non-existing bpnl
     * Then only create sites with existing bpnl
     */
    @Test
    fun `don't create sites with non-existing parent`() {


        val givenLegalEntities =
            poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1, BusinessPartnerNonVerboseValues.legalEntityCreate2)).entities

        val bpnL1 = givenLegalEntities.first().legalEntity.bpnl
        val bpnL2 = givenLegalEntities.last().legalEntity.bpnl


        val expected = listOf(BusinessPartnerVerboseValues.siteUpsert1, BusinessPartnerVerboseValues.siteUpsert2)

        val toCreate = listOf(
            BusinessPartnerNonVerboseValues.siteCreate1.copy(bpnlParent = bpnL1),
            BusinessPartnerNonVerboseValues.siteCreate2.copy(bpnlParent = bpnL2),
            BusinessPartnerNonVerboseValues.siteCreate3.copy(bpnlParent = "NONEXISTENT")
        )
        val response = poolClient.sites.createSite(toCreate)

        // 2 entities okay
        assertThatCreatedSitesEqual(response.entities, expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteCreateError.LegalEntityNotFound, BusinessPartnerVerboseValues.siteUpsert3.index!!)
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
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1))
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    siteStructures = listOf(SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate2), SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate3))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.site.bpns
        val bpnS2 = givenStructure[1].siteStructures[0].site.site.bpns
        val bpnS3 = givenStructure[1].siteStructures[1].site.site.bpns

        val expected = listOf(
            BusinessPartnerVerboseValues.siteUpsert1.run { copy(site = site.copy(bpns = bpnS3), index = null) },
            BusinessPartnerVerboseValues.siteUpsert2.run { copy(site = site.copy(bpns = bpnS1), index = null) },
            BusinessPartnerVerboseValues.siteUpsert3.run { copy(site = site.copy(bpns = bpnS2), index = null) },
        )

        val toUpdate = listOf(
            BusinessPartnerNonVerboseValues.siteUpdate1.copy(bpns = bpnS3),
            BusinessPartnerNonVerboseValues.siteUpdate2.copy(bpns = bpnS1),
            BusinessPartnerNonVerboseValues.siteUpdate3.copy(bpns = bpnS2)
        )

        val response = poolClient.sites.updateSite(toUpdate)

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
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1), SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate2))
                )
            )
        )

        val bpnS1 = givenStructure[0].siteStructures[0].site.site.bpns
        val bpnS2 = givenStructure[0].siteStructures[1].site.site.bpns

        val expected = listOf(
            BusinessPartnerVerboseValues.siteUpsert1.run { copy(site = site.copy(bpns = bpnS2), index = null) },
            BusinessPartnerVerboseValues.siteUpsert2.run { copy(site = site.copy(bpns = bpnS1), index = null) },
        )

        val toUpdate = listOf(
            BusinessPartnerNonVerboseValues.siteUpdate1.copy(bpns = bpnS2),
            BusinessPartnerNonVerboseValues.siteUpdate2.copy(bpns = bpnS1),
            BusinessPartnerNonVerboseValues.siteUpdate3.copy(bpns = "NONEXISTENT"),
        )
        val response = poolClient.sites.updateSite(toUpdate)

        // 2 entities okay
        assertThatCreatedSitesEqual(response.entities, expected)
        // 1 error
        assertThat(response.errorCount).isEqualTo(1)
        testHelpers.assertErrorResponse(response.errors.first(), SiteUpdateError.SiteNotFound, "NONEXISTENT")
    }

    @Test
    fun `retrieve sites with pagination`() {

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate1),
                        SiteStructureRequest(site = BusinessPartnerNonVerboseValues.siteCreate2)
                    )
                )
            )
        )

        val bpnL1 = createdStructures[0].legalEntity.legalEntity.bpnl

        val legalAddress1: LogisticAddressVerboseDto =
            BusinessPartnerVerboseValues.addressPartner1.copy(
                isMainAddress = true,
                addressType = AddressType.SiteMainAddress,
                bpnSite = BusinessPartnerVerboseValues.site1.bpns,
                bpna = "BPNA0000000001YN"
            )
        val site1 = BusinessPartnerVerboseValues.site1.copy(bpnLegalEntity = bpnL1)

        val legalAddress2: LogisticAddressVerboseDto =
            BusinessPartnerVerboseValues.addressPartner2.copy(
                isMainAddress = true,
                addressType = AddressType.SiteMainAddress,
                bpnSite = BusinessPartnerVerboseValues.site2.bpns,
                bpna = "BPNA0000000002XY"
            )
        val site2 = BusinessPartnerVerboseValues.site2.copy(bpnLegalEntity = bpnL1)

        val expectedFirstPage = PageDto(
            2, 1, 0, 2, listOf(
                SiteMatchVerboseDto(mainAddress = legalAddress1, site = site1),
                SiteMatchVerboseDto(mainAddress = legalAddress2, site = site2)
            )
        )

        val firstPage = poolClient.sites.getSitesPaginated(paginationRequest = PaginationRequest(0, 10))

        assertHelpers.assertRecursively(firstPage).ignoringFieldsOfTypes(Instant::class.java).isEqualTo(expectedFirstPage)

    }

    private fun assertThatCreatedSitesEqual(actuals: Collection<SitePartnerCreateVerboseDto>, expected: Collection<SitePartnerCreateVerboseDto>) {
        actuals.forEach { assertThat(it.site.bpns).matches(testHelpers.bpnSPattern) }

        assertHelpers.assertRecursively(actuals)
            .ignoringFields(
                SitePartnerCreateVerboseDto::site.name + "." + SiteVerboseDto::bpns.name,
                SitePartnerCreateVerboseDto::site.name + "." + SiteVerboseDto::bpnLegalEntity.name,
                SitePartnerCreateVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpna.name,
                SitePartnerCreateVerboseDto::mainAddress.name + "." + LogisticAddressVerboseDto::bpnSite.name,
                SitePartnerCreateVerboseDto::index.name
            )
            .isEqualTo(expected)
    }

    private fun requestSite(bpnSite: String) = poolClient.sites.getSite(bpnSite)

    private fun requestSitesOfLegalEntity(bpn: String) = poolClient.legalEntities.getSites(bpn, PaginationRequest())

}