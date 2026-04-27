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

package org.eclipse.tractusx.bpdm.pool.v7.changelog

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.junit.jupiter.api.Test
import java.time.Instant

class ChangelogParticipantSearchV7IT : UnscheduledPoolTestBaseV7() {

    private val anyTime = Instant.MIN

    /**
     * GIVEN changelog entries of participant data
     * WHEN participant searches for participant changelog entries
     * THEN participant sees participant changelog entries
     */
    @Test
    fun `search participant changelog entries`() {
        //GIVEN
        val legalEntityParticipantResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteParticipantResponse = testDataClient.createSite(legalEntityParticipantResponse, testName)
        val additionalAddressParticipant = testDataClient.createAdditionalAddress(siteParticipantResponse, testName)

        val legalEntityNonParticipantResponse = testDataClient.createLegalEntity("$testName NonParticipant")
        val siteNonParticipantResponse = testDataClient.createSite(legalEntityNonParticipantResponse, "$testName NonParticipant")
        testDataClient.createAdditionalAddress(siteNonParticipantResponse, "$testName NonParticipant")

        updateParticipantLegalEntity(legalEntityParticipantResponse, "Updated $testName")
        testDataClient.updateSite(siteParticipantResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddressParticipant)))

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(ChangelogSearchRequest(), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityParticipantResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityParticipantResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteParticipantResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteParticipantResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(additionalAddressParticipant.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),

            ChangelogEntryVerboseDto(legalEntityParticipantResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(legalEntityParticipantResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteParticipantResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteParticipantResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddressParticipant.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant changelog entries before time X and changelog entries after time X
     * WHEN participant searches for participant changelog entries after time X
     * THEN participant only sees participant entries after time X
     */
    @Test
    fun `filter participant changelog entries after timestamp`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        val timeX = Instant.now()

        val additionalAddress = testDataClient.createAdditionalAddress(siteResponse, testName)

        updateParticipantLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddress)))

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(ChangelogSearchRequest(timestampAfter = timeX), PaginationRequest(0, 20))

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(legalEntityResponse.legalAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.mainAddress.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE)
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant changelog entries
     * WHEN participant searches for participant changelog entries by BPNs
     * THEN participant only sees participant entries having one of the given BPNs
     */
    @Test
    fun `filter participant changelog entries by BPN`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddress(siteResponse, testName)

        updateParticipantLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddress)))

        testDataClient.createParticipantLegalEntity("Other $testName")
        testDataClient.createSite(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(
            ChangelogSearchRequest(
                bpns = setOf(
                    legalEntityResponse.header.bpnl,
                    siteResponse.site.bpns,
                    additionalAddress.address.bpna
                )
            ),
            PaginationRequest(0, 20)
        )

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.CREATE),

            ChangelogEntryVerboseDto(legalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(siteResponse.site.bpns, BusinessPartnerType.SITE, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(additionalAddress.address.bpna, BusinessPartnerType.ADDRESS, anyTime, ChangelogType.UPDATE),
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant changelog entries
     * WHEN participant searches for participant legal entity changelog entries
     * THEN participant only sees participant legal entity entries
     */
    @Test
    fun `filter participant legal entity changelog entries`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddress(siteResponse, testName)

        updateParticipantLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddress)))

        val otherLegalEntityResponse = testDataClient.createParticipantLegalEntity("Other $testName")
        testDataClient.createSite(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(
            ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.LEGAL_ENTITY)),
            PaginationRequest(0, 20)
        )

        //THEN
        val expectedEntries = listOf(
            ChangelogEntryVerboseDto(legalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
            ChangelogEntryVerboseDto(legalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.UPDATE),
            ChangelogEntryVerboseDto(otherLegalEntityResponse.header.bpnl, BusinessPartnerType.LEGAL_ENTITY, anyTime, ChangelogType.CREATE),
        )
        val expectedResponse = PageDto(expectedEntries.size.toLong(), 1, 0, expectedEntries.size, expectedEntries)

        assertChangelogs(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant changelog entries
     * WHEN participant searches for participant site changelog entries
     * THEN participant only sees participant site entries
     */
    @Test
    fun `filter participant site changelog entries`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddress(siteResponse, testName)

        updateParticipantLegalEntity(legalEntityResponse, "Updated $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddress)))

        testDataClient.createParticipantLegalEntity("Other $testName")
        val otherSiteResponse = testDataClient.createSite(legalEntityResponse, "Other $testName")

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(
            ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.SITE)),
            PaginationRequest(0, 20)
        )

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
     * GIVEN participant changelog entries
     * WHEN participant searches for participant address changelog entries
     * THEN participant only sees participant address entries
     */
    @Test
    fun `filter participant address changelog entries`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val additionalAddress = testDataClient.createAdditionalAddress(siteResponse, testName)

        updateParticipantLegalEntity(legalEntityResponse, "Updated Legal Entity $testName")
        testDataClient.updateSite(siteResponse, "Updated $testName")
        poolClient.addresses.updateAddresses(listOf(requestFactory.buildAddressUpdateRequest("Updated $testName", additionalAddress)))

        //WHEN
        val searchResponse = poolClient.members.searchChangelogEntries(
            ChangelogSearchRequest(businessPartnerTypes = setOf(BusinessPartnerType.ADDRESS)),
            PaginationRequest(0, 20)
        )

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

    private fun assertChangelogs(actual: PageDto<ChangelogEntryVerboseDto>, expected: PageDto<ChangelogEntryVerboseDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("${PageDto<*>::content.name}.${ChangelogEntryVerboseDto::timestamp.name}")
            .isEqualTo(expected)
    }

    private fun updateParticipantLegalEntity(legalEntityToUpdate: LegalEntityWithLegalAddressVerboseDto, seed: String): LegalEntityWithLegalAddressVerboseDto {
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest(seed, legalEntityToUpdate.header.bpnl).withParticipantData(true)
        return  poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest)).entities.first().legalEntity
    }
}
