/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v6.changelog

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogEntryVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogTypeV6
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class ChangelogSearchV6IT: UnscheduledPoolTestBaseV6() {

    private val anyTime = Instant.MIN

    private val searchRequestLimit = 100


    /**
     * GIVEN changelog entries
     * WHEN sharing member searches for unfiltered changelog entries
     * THEN sharing member sees all changelog entries
     */
    @Test
    fun `search all changelog entries`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),

            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN changelog entries before time X and changelog entries after time X
     * WHEN sharing member searches for changelog entries after time X
     * THEN sharing member only sees entries after time X
     */
    @Test
    fun `filter changelog entries after timestamp`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        val timeX = Instant.now()

        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(timestampAfter = timeX), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN changelog entries
     * WHEN sharing member searches for changelog entries by BPNs
     * THEN sharing member only sees entries having one of the given BPNs
     */
    @Test
    fun `filter changelog entries by BPN`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        testDataClient.createLegalEntity("Other $testName")
        testDataClient.createSiteFor(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(bpns = setOf(legalEntityResponse.legalEntity.bpnl, siteResponse.site.bpns, additionalAddress.address.bpna)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),

            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN changelog entries
     * WHEN sharing member searches for legal entity changelog entries
     * THEN sharing member only sees legal entity entries
     */
    @Test
    fun `filter legal entity changelog entries`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        val otherLegalEntityResponse = testDataClient.createLegalEntity("Other $testName")
        testDataClient.createSiteFor(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(businessPartnerTypes = setOf(BusinessPartnerType.LEGAL_ENTITY)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(otherLegalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogTypeV6.CREATE),

        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN changelog entries
     * WHEN sharing member searches for site changelog entries
     * THEN sharing member only sees site entries
     */
    @Test
    fun `filter site changelog entries`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        testDataClient.createLegalEntity("Other $testName")
        val otherSiteResponse = testDataClient.createSiteFor(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(businessPartnerTypes = setOf(BusinessPartnerType.SITE)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(otherSiteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogTypeV6.CREATE),
            )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN changelog entries
     * WHEN sharing member searches for address changelog entries
     * THEN sharing member only sees address entries
     */
    @Test
    fun `filter address changelog entries`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        testDataClient.updateLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        testDataClient.updateAddress(additionalAddress, "Updated $testName")

        //WHEN
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(businessPartnerTypes = setOf(BusinessPartnerType.ADDRESS)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.CREATE),

            ChangelogEntryVerboseV6Dto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE),
            ChangelogEntryVerboseV6Dto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogTypeV6.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN a search request filtering by more BPNs than the search request limit allows
     * WHEN sharing member searches for changelog entries
     * THEN 400 bad request is returned
     */
    @Test
    fun `filter changelog entries by too many BPNs returns 400 bad request`() {
        //GIVEN
        val tooManyBpns = (1..searchRequestLimit + 1).map { "BPNL$it" }.toSet()

        //WHEN / THEN
        Assertions.assertThatThrownBy {
            poolClient.changelogs.getChangelogEntries(ChangelogSearchV6Request(bpns = tooManyBpns), PaginationRequest(0, 20))
        }.isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun assertChangelogs(actual: PageDto<ChangelogEntryVerboseV6Dto>, expected: PageDto<ChangelogEntryVerboseV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("${PageDto<*>::content.name}.${ChangelogEntryVerboseV6Dto::timestamp.name}")
            .isEqualTo(expected)
    }

}