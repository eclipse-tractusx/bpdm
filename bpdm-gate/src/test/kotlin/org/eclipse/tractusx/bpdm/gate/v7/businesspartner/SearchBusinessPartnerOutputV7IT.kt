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

package org.eclipse.tractusx.bpdm.gate.v7.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.api.model.response.AdditionalSiteOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.gate.service.GoldenRecordUpdateChunkService
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class SearchBusinessPartnerOutputV7IT: UnscheduledGateTestBaseV7() {

    @Autowired
    lateinit var goldenRecordUpdateService: GoldenRecordUpdateChunkService

    /**
     * GIVEN an output following a site that need not be the Pool address's oldest site
     * WHEN the Pool reports an address change
     * THEN the output type reflects its tracked site's main address and the legal-address role
     */
    @ParameterizedTest
    @CsvSource(
        "SiteMainAddress, false, AdditionalAddress",
        "SiteMainAddress, true, SiteMainAddress",
        "LegalAndSiteMainAddress, false, LegalAddress",
        "LegalAndSiteMainAddress, true, LegalAndSiteMainAddress",
        "LegalAddress, false, LegalAddress",
        "AdditionalAddress, false, AdditionalAddress"
    )
    fun `pool address sync determines type towards the tracked site`(
        poolType: AddressType,
        trackedSiteMain: Boolean,
        expectedType: AddressType
    ) {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val before = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        assertEquals(AddressType.AdditionalAddress, before.address.addressType)

        val changedAddress = goldenRecord.additionalAddress.copy(
            address = goldenRecord.additionalAddress.address.copy(addressType = poolType, updatedAt = Instant.now())
        )
        val trackedSite = goldenRecord.siteParent.let {
            if (trackedSiteMain) it.copy(mainAddress = changedAddress.address) else it
        }
        val otherSite = goldenRecord.additionalSites.single().let {
            if (poolType == AddressType.SiteMainAddress || poolType == AddressType.LegalAndSiteMainAddress)
                it.copy(mainAddress = changedAddress.address)
            else it
        }
        poolMockDataFactory.mockSiteSearchResult(listOf(trackedSite, otherSite))
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseDto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogType.UPDATE))
        )

        goldenRecordUpdateService.updateFromNextChunk()

        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId))
        val expected = testData.businessPartner.output.fromAdditionalAddressOnSite(
            input, goldenRecord.legalEntityParent, trackedSite, changedAddress,
            listOf(AdditionalSiteOutputDto(otherSite.site.bpns, otherSite.site.name))
        ).let { it.copy(address = it.address.copy(addressType = expectedType)) }
        assertRepo.assertBusinessPartnerOutput(response, PageDto(1, 1, 0, 1, listOf(expected)))
    }

    /**
     * GIVEN two outputs sharing an address but following different sites
     * WHEN an address changelog is synchronized
     * THEN only the output following the address's main site reports SiteMainAddress
     */
    @Test
    fun `pool address sync preserves different roles of the same address across outputs`() {
        val inputB = testDataClient.businessPartner.upsertInput("$testName B")
        val goldenRecord = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(inputB)
        val siteA = goldenRecord.additionalSites.single().copy(mainAddress = goldenRecord.additionalAddress.address)
        val inputA = testDataClient.businessPartner.upsertInput("$testName A")
        orchestratorMockDataFactory.mockRefineToSite(
            inputA.externalId, goldenRecord.legalEntityParent, siteA, null, inputA.nameParts
        )
        testDataClient.businessPartner.setStateToReady(inputA.externalId)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()

        val changedAddress = goldenRecord.additionalAddress.copy(
            address = goldenRecord.additionalAddress.address.copy(addressType = AddressType.SiteMainAddress, updatedAt = Instant.now())
        )
        poolMockDataFactory.mockSiteSearchResult(listOf(siteA, goldenRecord.siteParent))
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseDto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogType.UPDATE))
        )

        goldenRecordUpdateService.updateFromNextChunk()

        val outputs = gateClient.businessParters.getBusinessPartnersOutput(listOf(inputA.externalId, inputB.externalId))
            .content.associateBy { it.externalId }
        val outputA = outputs.getValue(inputA.externalId)
        val outputB = outputs.getValue(inputB.externalId)
        assertEquals(changedAddress.address.bpna, outputA.address.addressBpn)
        assertEquals(changedAddress.address.bpna, outputB.address.addressBpn)
        assertEquals(siteA.site.bpns, outputA.site?.siteBpn)
        assertEquals(goldenRecord.siteParent.site.bpns, outputB.site?.siteBpn)
        assertEquals(AddressType.SiteMainAddress, outputA.address.addressType)
        assertEquals(AddressType.AdditionalAddress, outputB.address.addressType)
        assertEquals(listOf(AdditionalSiteOutputDto(goldenRecord.siteParent.site.bpns, goldenRecord.siteParent.site.name)), outputA.additionalSites)
        assertEquals(listOf(AdditionalSiteOutputDto(siteA.site.bpns, siteA.site.name)), outputB.additionalSites)

        goldenRecordUpdateService.updateFromNextChunk()
        val unchangedOutputs = gateClient.businessParters.getBusinessPartnersOutput(listOf(inputA.externalId, inputB.externalId))
            .content.associateBy { it.externalId }
        assertEquals(outputs, unchangedOutputs)
    }

    @Test
    fun `pool address sync does not guess the role when the tracked site is missing`() {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val before = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        val changedAddress = goldenRecord.additionalAddress.copy(
            address = goldenRecord.additionalAddress.address.copy(addressType = AddressType.SiteMainAddress, updatedAt = Instant.now())
        )
        poolMockDataFactory.mockSiteSearchResult(goldenRecord.additionalSites)
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseDto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogType.UPDATE))
        )

        goldenRecordUpdateService.updateFromNextChunk()

        val after = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        assertEquals(before, after)
    }

    @Test
    fun `pool address sync preserves legal address without a site`() {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToLegalEntity(input)
        val changedAddress = LogisticAddressVerboseDto(
            goldenRecord.legalAddress.copy(updatedAt = Instant.now()), emptyList()
        )
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseDto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogType.UPDATE))
        )

        goldenRecordUpdateService.updateFromNextChunk()

        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId))
        val expected = testData.businessPartner.output.fromLegalEntity(
            input, goldenRecord.copy(legalAddress = changedAddress.address)
        )
        assertRepo.assertBusinessPartnerOutput(response, PageDto(1, 1, 0, 1, listOf(expected)))
    }

    @ParameterizedTest
    @CsvSource("true, LegalAndSiteMainAddress", "false, LegalAddress")
    fun `pool address sync determines type towards a newly adopted site`(siteMain: Boolean, expectedType: AddressType) {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val legalEntity = testDataClient.businessPartner.refineToLegalEntity(input)
        val site = poolMockDataFactory.mockSiteAndMainAddressSearchResult("$testName Site").site
        val changedAddress = LogisticAddressVerboseDto(
            legalEntity.legalAddress.copy(
                bpnSite = site.site.bpns,
                addressType = if (siteMain) AddressType.LegalAndSiteMainAddress else AddressType.LegalAddress,
                updatedAt = Instant.now()
            ), emptyList()
        )
        val adoptedSite = site.copy(
            site = site.site.copy(bpnLegalEntity = legalEntity.header.bpnl),
            mainAddress = if (siteMain) changedAddress.address else site.mainAddress
        )
        poolMockDataFactory.mockSiteSearchResult(listOf(adoptedSite))
        poolMockDataFactory.mockAddressSearchResult(changedAddress)
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseDto(changedAddress.address.bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogType.UPDATE))
        )

        goldenRecordUpdateService.updateFromNextChunk()

        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        assertEquals(adoptedSite.site.bpns, output.site?.siteBpn)
        assertEquals(adoptedSite.site.name, output.site?.name)
        assertEquals(legalEntity.header.bpnl, output.legalEntity.legalEntityBpn)
        assertEquals(changedAddress.address.bpna, output.address.addressBpn)
        assertEquals(expectedType, output.address.addressType)
        assertEquals(emptyList<AdditionalSiteOutputDto>(), output.additionalSites)
    }

    /**
     * GIVEN business partner output based on legal entity
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees received golden record result
     *
     */
    @Test
    fun `search legal entity business partner output`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val legalEntityGoldenRecord = testDataClient.businessPartner.refineToLegalEntity(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput =  testData.businessPartner.output.fromLegalEntity(createdInput, legalEntityGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on legal entity that is also a site main address
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with both legal entity and site references
     *
     */
    @Test
    fun `search legal entity on site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToLegalEntityOnSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromLegalEntityOnSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.site)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on site main address
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with site address and legal entity reference
     *
     */
    @Test
    fun `search site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.site)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on additional address belonging to a site
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees golden record result with additional address, site, and legal entity references
     *
     */
    @Test
    fun `search additional address of site business partner output`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToAdditionalAddressOfSite(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromAdditionalAddressOnSite(createdInput, goldenRecord.legalEntityParent, goldenRecord.siteParent, goldenRecord.additionalAddress)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output based on an additional address that belongs to further sites beyond its primary site
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees the further site memberships in the additionalSites collection, resolved to name/BPN pairs
     *
     */
    @Test
    fun `search additional address business partner output with additional sites`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val goldenRecord = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedAdditionalSites = goldenRecord.additionalSites.map { AdditionalSiteOutputDto(it.site.bpns, it.site.name) }
        val expectedOutput = testData.businessPartner.output.fromAdditionalAddressOnSite(
            createdInput, goldenRecord.legalEntityParent, goldenRecord.siteParent, goldenRecord.additionalAddress, expectedAdditionalSites
        )
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN no business partner output for a given external-ID
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees an empty result
     *
     */
    @Test
    fun `search business partner output by non-existing external-ID`(){
        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf("non-existing-external-id"))

        //THEN
        val expectedResponse = PageDto(0, 0, 0, 0, emptyList<BusinessPartnerOutputDto>())

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }

    /**
     * GIVEN business partner output exists for one external-ID but not for another
     * WHEN output consumer searches for outputs with both external-IDs
     * THEN output consumer sees only the output for the existing external-ID
     *
     */
    @Test
    fun `search business partner output filters out non-existing external-IDs`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val legalEntityGoldenRecord = testDataClient.businessPartner.refineToLegalEntity(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId, "non-existing-external-id"))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromLegalEntity(createdInput, legalEntityGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }
}