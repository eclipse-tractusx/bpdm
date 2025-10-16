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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.changelog

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test
import java.time.Instant

class ChangelogSearchIT: SharingMemberTest() {

    private val anyTime = Instant.MIN


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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),

            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE)
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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(timestampAfter = timeX), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE)
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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(bpns = setOf(legalEntityResponse.legalEntity.bpnl, siteResponse.site.bpns, additionalAddress.address.bpna)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),

            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.LEGAL_ENTITY)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(otherLegalEntityResponse.legalEntity.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),

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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.SITE)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(otherSiteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
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
        val searchResponse = poolClient.changelogs.getChangelogEntries(ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.ADDRESS)), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),

            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    private fun assertChangelogs(actual: PageDto<ChangelogEntryVerboseDto>, expected: PageDto<ChangelogEntryVerboseDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("${PageDto<*>::content.name}.${ChangelogEntryVerboseDto::timestamp.name}")
            .isEqualTo(expected)
    }

}