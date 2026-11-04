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

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.AdditionalSiteOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.BusinessPartnerOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.v7.UnscheduledGateTestBaseV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class SearchBusinessPartnerOutputV7IT: UnscheduledGateTestBaseV7() {

    private val siteRelativeAddressComparison = RecursiveComparisonConfiguration.builder()
        .withComparedFields("externalId", "site.siteBpn", "address.addressBpn", "address.name", "address.addressType", "additionalSites")
        .build()

    /**
     * GIVEN a business partner output whose additional address is returned by the Pool with a given global address type
     * WHEN the output consumer searches for that output
     * THEN the output reports the address type relative to its tracked site:
     *      the site-main role only applies if the tracked site's main address is this address,
     *      while the legal-address role is preserved regardless of the tracked site
     *
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
    fun `pool sync determines address type towards the tracked site`(
        poolType: AddressType, trackedSiteMain: Boolean, expectedType: AddressType
    ) {
        //GIVEN an output tracking a site where the Pool reports the address with the given global type
        val input = testDataClient.businessPartner.upsertInput(testName)
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val changedAddress = records.additionalAddress.copy(
            scriptVariants = emptyList(),
            address = records.additionalAddress.address.copy(name = "$testName changed", addressType = poolType, updatedAt = Instant.now())
        )
        val trackedSite = records.siteParent.let { if (trackedSiteMain) it.copy(mainAddress = changedAddress.address) else it }
        val otherSite = records.additionalSites.single().let {
            if (poolType == AddressType.SiteMainAddress || poolType == AddressType.LegalAndSiteMainAddress)
                it.copy(mainAddress = changedAddress.address)
            else it
        }
        testDataClient.businessPartner.syncAddressChangeFromPool(changedAddress, listOf(trackedSite, otherSite))

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromAdditionalAddressOnSite(
            input,
            records.legalEntityParent,
            records.siteParent,
            records.additionalAddress.copy(address = changedAddress.address),
            listOf(AdditionalSiteOutputV7Dto(otherSite.site.bpns, otherSite.site.name))
        ).let { it.copy(address = it.address.copy(addressType = expectedType)) }
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse, siteRelativeAddressComparison)
    }

    /**
     * GIVEN two business partner outputs sharing the same Pool address that is the main address of site A but only
     *       an additional address of site B, and a change of that shared address synchronized from the Pool
     * WHEN the output consumer searches for both outputs
     * THEN both outputs report the changed address, each keeping its tracked site, its further site membership and
     *      its own site-relative address type
     *
     */
    @Test
    fun `pool sync keeps site-relative address types for two outputs sharing an address`() {
        //GIVEN
        val shared = givenOutputsSharingAnAddress()

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(shared.inputA.externalId, shared.inputB.externalId))

        //THEN
        assertRepo.assertBusinessPartnerOutput(response, shared.expectedPage(testData), siteRelativeAddressComparison)
    }

    /**
     * GIVEN two business partner outputs sharing the same Pool address whose change was synchronized twice
     * WHEN the output consumer searches for both outputs
     * THEN both outputs still report the same site-relative address types
     *
     */
    @Test
    fun `repeated pool sync keeps site-relative address types stable`() {
        //GIVEN
        val shared = givenOutputsSharingAnAddress()
        testDataClient.businessPartner.syncAddressChangeFromPool(shared.changedAddress, listOf(shared.siteA, shared.records.siteParent))

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(shared.inputA.externalId, shared.inputB.externalId))

        //THEN
        assertRepo.assertBusinessPartnerOutput(response, shared.expectedPage(testData), siteRelativeAddressComparison)
    }

    /**
     * GIVEN a business partner output whose tracked site no longer lists the address as a member,
     *       even though the Pool address type still claims a site-main role
     * WHEN the output consumer searches for that output
     * THEN the address change is still applied and the tracked site is preserved,
     *      but the output does not report a site-main role for the removed membership;
     *      the legal-address role is preserved if present
     *
     */
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
        //GIVEN the Pool reports the address with a site-main type but without membership in the tracked site
        val input = testDataClient.businessPartner.upsertInput(testName)
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(input)
        val changedAddress = records.additionalAddress.copy(
            scriptVariants = emptyList(),
            address = records.additionalAddress.address.copy(
                name = "$testName changed",
                addressType = poolType,
                additionalSites = emptyList(),
                updatedAt = Instant.now()
            )
        )
        val otherSite = records.additionalSites.single().copy(mainAddress = changedAddress.address)
        val trackedSite = records.siteParent.copy(mainAddress = changedAddress.address)
        testDataClient.businessPartner.syncAddressChangeFromPool(
            changedAddress, if (trackedSiteReturned) listOf(otherSite, trackedSite) else listOf(otherSite)
        )

        //WHEN
        val response = gateClient.businessParters.getBusinessPartnersOutput(listOf(input.externalId))

        //THEN
        val expectedOutput = testData.businessPartner.output.fromAdditionalAddressOnSite(
            input,
            records.legalEntityParent,
            records.siteParent,
            records.additionalAddress.copy(address = changedAddress.address),
            listOf(AdditionalSiteOutputV7Dto(otherSite.site.bpns, otherSite.site.name))
        ).let { it.copy(address = it.address.copy(addressType = expectedType)) }
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse, siteRelativeAddressComparison)
    }

    private class SharedAddressScenario(
        val inputA: BusinessPartnerInputV7Dto,
        val inputB: BusinessPartnerInputV7Dto,
        val records: PoolMockDataFactory.AdditionalAddressWithAdditionalSitesResult,
        val siteA: SiteWithMainAddressVerboseV7Dto,
        val changedAddress: LogisticAddressVerboseV7Dto
    ) {
        fun expectedPage(testData: TestDataFactoryGateV7): PageDto<BusinessPartnerOutputV7Dto> {
            val expectedA = testData.businessPartner.output.fromSite(inputA, records.legalEntityParent, siteA.copy(mainAddress = changedAddress.address))
                .copy(additionalSites = listOf(AdditionalSiteOutputV7Dto(records.siteParent.site.bpns, records.siteParent.site.name)))
            val expectedB = testData.businessPartner.output.fromAdditionalAddressOnSite(
                inputB,
                records.legalEntityParent,
                records.siteParent,
                records.additionalAddress.copy(address = changedAddress.address),
                listOf(AdditionalSiteOutputV7Dto(siteA.site.bpns, siteA.site.name))
            )
            return PageDto(2, 1, 0, 2, listOf(expectedA, expectedB))
        }
    }

    private fun givenOutputsSharingAnAddress(): SharedAddressScenario {
        // output B tracks the site where the shared address is only an additional address
        val inputB = testDataClient.businessPartner.upsertInput("$testName B")
        val records = testDataClient.businessPartner.refineToAdditionalAddressWithAdditionalSites(inputB)
        val siteA = records.additionalSites.single().copy(mainAddress = records.additionalAddress.address)

        // the Pool agrees that the shared address is site A's main address
        poolMockDataFactory.mockSiteSearchResult(listOf(siteA, records.siteParent))

        // output A tracks site A where the shared address is the main address
        val inputA = testDataClient.businessPartner.upsertInput("$testName A")
        orchestratorMockDataFactory.mockRefineToSite(inputA.externalId, records.legalEntityParent, siteA, null, inputA.nameParts)
        testDataClient.businessPartner.setStateToReady(inputA.externalId)
        taskCreationService.createTasksForReadyBusinessPartners()
        taskResolutionService.resolveFinishedTasks()

        // the Pool reports a change on the shared address
        val changedAddress = records.additionalAddress.copy(
            scriptVariants = emptyList(),
            address = records.additionalAddress.address.copy(
                name = "$testName changed", addressType = AddressType.SiteMainAddress, updatedAt = Instant.now()
            )
        )
        testDataClient.businessPartner.syncAddressChangeFromPool(changedAddress, listOf(siteA, records.siteParent))

        return SharedAddressScenario(inputA, inputB, records, siteA, changedAddress)
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
        val expectedOutput =  testData.businessPartner.output.fromLegalEntity(createdInput, legalEntityGoldenRecord)
        val expectedResponse = PageDto(1, 1, 0, 1, listOf(expectedOutput))

        assertRepo.assertBusinessPartnerOutput(response, expectedResponse)
    }
}
