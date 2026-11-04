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
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.AdditionalSiteOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.BusinessPartnerOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.gate.service.application.GoldenRecordUpdateApplicationService
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogTypeV7
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class SearchBusinessPartnerOutputV7IT: UnscheduledGateTestBaseV7() {

    @Autowired
    lateinit var goldenRecordUpdateService: GoldenRecordUpdateApplicationService

    @ParameterizedTest
    @CsvSource(
        "SiteMainAddress, false, AdditionalAddress",
        "SiteMainAddress, true, SiteMainAddress",
        "LegalAndSiteMainAddress, false, LegalAddress",
        "LegalAndSiteMainAddress, true, LegalAndSiteMainAddress",
        "LegalAddress, false, LegalAddress",
        "AdditionalAddress, false, AdditionalAddress"
    )
    fun `pool sync determines address type towards the tracked site`(
        poolType: AddressType, trackedSiteMain: Boolean, expectedType: AddressType
    ) {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val address = records.additionalAddress.copy(
            address = records.additionalAddress.address.copy(addressType = poolType, updatedAt = Instant.now())
        )
        val trackedSite = records.siteParent.let { if (trackedSiteMain) it.copy(mainAddress = address.address) else it }
        val otherSite = records.additionalSites.single().let {
            if (poolType == AddressType.SiteMainAddress || poolType == AddressType.LegalAndSiteMainAddress)
                it.copy(mainAddress = address.address)
            else it
        }
        poolMockDataFactory.mockSiteSearchResult(listOf(trackedSite, otherSite))
        poolMockDataFactory.mockAddressSearchResult(address)
        syncAddress(address.address.bpna)

        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        assertEquals(expectedType, output.address.addressType)
        assertEquals(trackedSite.site.bpns, output.site?.siteBpn)
        assertEquals(address.address.bpna, output.address.addressBpn)
        assertEquals(listOf(AdditionalSiteOutputV7Dto(otherSite.site.bpns, otherSite.site.name)), output.additionalSites)
    }

    @Test
    fun `pool sync preserves different roles for two outputs sharing an address`() {
        val inputB = testDataClient.businessPartner.upsertInput("$testName B")
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(inputB)
        val siteA = records.additionalSites.single().copy(mainAddress = records.additionalAddress.address)
        val inputA = testDataClient.businessPartner.upsertInput("$testName A")
        poolMockDataFactory.mockSiteSearchResult(listOf(siteA, records.siteParent))
        orchestratorMockDataFactory.mockRefineToSite(
            inputA.externalId, records.legalEntityParent, siteA, null, inputA.nameParts
        )
        testDataClient.businessPartner.setStateToReady(inputA.externalId)
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()

        val address = records.additionalAddress.copy(
            address = records.additionalAddress.address.copy(
                name = "$testName changed", addressType = AddressType.SiteMainAddress, updatedAt = Instant.now()
            )
        )
        poolMockDataFactory.mockAddressSearchResult(address)
        syncAddress(address.address.bpna)

        val outputs = gateClient.businessParters.getBusinessPartnersOutput(listOf(inputA.externalId, inputB.externalId))
            .content.associateBy { it.externalId }
        val a = outputs.getValue(inputA.externalId)
        val b = outputs.getValue(inputB.externalId)
        assertEquals(address.address.bpna, a.address.addressBpn)
        assertEquals(address.address.bpna, b.address.addressBpn)
        assertEquals(address.address.name, a.address.name)
        assertEquals(address.address.name, b.address.name)
        assertEquals(siteA.site.bpns, a.site?.siteBpn)
        assertEquals(records.siteParent.site.bpns, b.site?.siteBpn)
        assertEquals(AddressType.SiteMainAddress, a.address.addressType)
        assertEquals(AddressType.AdditionalAddress, b.address.addressType)
        assertEquals(listOf(AdditionalSiteOutputV7Dto(records.siteParent.site.bpns, records.siteParent.site.name)), a.additionalSites)
        assertEquals(listOf(AdditionalSiteOutputV7Dto(siteA.site.bpns, siteA.site.name)), b.additionalSites)

        syncAddress(address.address.bpna)
        val repeated = gateClient.businessParters.getBusinessPartnersOutput(listOf(inputA.externalId, inputB.externalId))
            .content.associateBy { it.externalId }
        assertEquals(outputs, repeated)
    }

    @Test
    fun `pool sync skips address update when the tracked site is missing`() {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val before = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        poolMockDataFactory.mockSiteSearchResult(records.additionalSites)
        poolMockDataFactory.mockAddressSearchResult(records.additionalAddress.copy(
            address = records.additionalAddress.address.copy(addressType = AddressType.SiteMainAddress, updatedAt = Instant.now())
        ))
        syncAddress(records.additionalAddress.address.bpna)

        assertEquals(before, gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single())
    }

    @ParameterizedTest
    @CsvSource(
        "SiteMainAddress, AdditionalAddress, false",
        "SiteMainAddress, AdditionalAddress, true",
        "LegalAndSiteMainAddress, LegalAddress, false",
        "LegalAndSiteMainAddress, LegalAddress, true"
    )
    fun `pool sync applies address changes after tracked site membership is removed`(
        poolType: AddressType, expectedType: AddressType, trackedSiteReturned: Boolean
    ) {
        val input = testDataClient.businessPartner.upsertInput(testName)
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val before = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        val address = records.additionalAddress.copy(
            address = records.additionalAddress.address.copy(
                name = "$testName changed",
                addressType = poolType,
                additionalSites = emptyList(),
                updatedAt = Instant.now()
            )
        )
        val otherSite = records.additionalSites.single().copy(mainAddress = address.address)
        // Even a returned site must not imply a main-address role after membership was removed.
        val trackedSite = records.siteParent.copy(mainAddress = address.address)
        poolMockDataFactory.mockSiteSearchResult(
            if (trackedSiteReturned) listOf(otherSite, trackedSite) else listOf(otherSite)
        )
        poolMockDataFactory.mockAddressSearchResult(address)
        syncAddress(address.address.bpna)

        val output = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId)).content.single()
        assertEquals(before.site, output.site)
        assertEquals(address.address.bpna, output.address.addressBpn)
        assertEquals(address.address.name, output.address.name)
        assertEquals(address.address.updatedAt, output.address.updatedAt)
        assertEquals(expectedType, output.address.addressType)
        assertEquals(listOf(AdditionalSiteOutputV7Dto(otherSite.site.bpns, otherSite.site.name)), output.additionalSites)
    }

    private fun syncAddress(bpna: String) {
        poolMockDataFactory.mockChangelogSearchResult(
            listOf(ChangelogEntryVerboseV7Dto(bpna, BusinessPartnerType.ADDRESS, Instant.now(), ChangelogTypeV7.UPDATE))
        )
        goldenRecordUpdateService.updateFromPoolChanges()
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
     * GIVEN business partner output based on legal entity whose golden record the Pool changed while its task was pending
     * WHEN output consumer searches for output with that external-ID
     * THEN output consumer sees the changed golden record instead of the task result
     *
     */
    @Test
    fun `search legal entity business partner output changed while task was pending`(){
        //GIVEN
        val createdInput = testDataClient.businessPartner.upsertInput(testName)
        val changedGoldenRecord = testDataClient.businessPartner.refineToLegalEntityChangedInPool(createdInput)

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(createdInput.externalId))

        //THEN
        val expectedOutput =  testData.businessPartner.output.fromLegalEntity(createdInput, changedGoldenRecord)
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
        val expectedAdditionalSites = goldenRecord.additionalSites.map { AdditionalSiteOutputV7Dto(it.site.bpns, it.site.name) }
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
        val expectedResponse = PageDto(0, 0, 0, 0, emptyList<BusinessPartnerOutputV7Dto>())

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