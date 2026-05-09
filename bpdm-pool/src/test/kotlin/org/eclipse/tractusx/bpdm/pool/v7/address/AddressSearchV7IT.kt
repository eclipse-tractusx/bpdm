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

package org.eclipse.tractusx.bpdm.pool.v7.address

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class AddressSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN addresses
     * WHEN sharing member searches for all addresses
     * THEN sharing member sees all addresses
     */
    @Test
    fun `search addresses`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSite(legalEntityResponse, "Legal Site $testName")
        val siteResponse = testDataClient.createSite(legalEntityResponse, "Site $testName")
        val additionalAddressResponse = testDataClient.createAdditionalAddress(legalEntityResponse, "Additional Address $testName")
        val additionalSiteAddressResponse = testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchResponseGet = poolClient.addresses.getAddresses(AddressSearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(AddressSearchRequest(), PaginationRequest())

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponseFromLegalSite(legalAddressSiteResponse, legalEntityResponse),
            resultFactory.buildAddressSearchResponse(siteResponse),
            resultFactory.buildAddressSearchResponse(additionalAddressResponse),
            resultFactory.buildAddressSearchResponse(additionalSiteAddressResponse)
        )
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by BPNA
     * THEN sharing member sees only addresses with these BPNAs
     */
    @Test
    fun `search addresses by BPNA`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSite(legalEntityResponse, "Legal Site $testName")
        val siteResponse = testDataClient.createSite(legalEntityResponse, "Site $testName")
        val additionalAddressResponse = testDataClient.createAdditionalAddress(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(addressBpns = listOf(legalAddressSiteResponse.mainAddress.bpna, additionalAddressResponse.address.bpna))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponseFromLegalSite(legalAddressSiteResponse, legalEntityResponse),
            resultFactory.buildAddressSearchResponse(additionalAddressResponse)
        )
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by their legal entity BPNLs
     * THEN sharing member sees only addresses from these legal entities
     */
    @Test
    fun `search addresses by BPNLs`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createParticipantLegalEntity("A $testName")
        val legalAddressSiteResponseA = testDataClient.createLegalAddressSite(legalEntityResponseA, "Legal Site $testName")
        val siteResponseA = testDataClient.createSite(legalEntityResponseA, "Site $testName")
        val additionalAddressResponseA = testDataClient.createAdditionalAddress(legalEntityResponseA, "Additional Address $testName")
        val additionalSiteAddressResponseA = testDataClient.createAdditionalAddress(siteResponseA, "Additional Site Address $testName")

        val legalEntityResponseB = testDataClient.createParticipantLegalEntity("B $testName")
        val siteResponseB = testDataClient.createSite(legalEntityResponseB, "Site B $testName")
        testDataClient.createAdditionalAddress(siteResponseB, "Additional Address B $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(legalEntityResponseA.header.bpnl))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponseFromLegalSite(legalAddressSiteResponseA, legalEntityResponseA),
            resultFactory.buildAddressSearchResponse(siteResponseA),
            resultFactory.buildAddressSearchResponse(additionalAddressResponseA),
            resultFactory.buildAddressSearchResponse(additionalSiteAddressResponseA)
        )
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by their site BPNS
     * THEN sharing member sees only addresses from these sites
     */
    @Test
    fun `search addresses by BPNS`() {
        //GIVEN
        val legalEntityResponseA = testDataClient.createParticipantLegalEntity("A $testName")
        testDataClient.createLegalAddressSite(legalEntityResponseA, "Legal Site $testName")
        val siteResponseA = testDataClient.createSite(legalEntityResponseA, "Site $testName")
        testDataClient.createAdditionalAddress(legalEntityResponseA, "Additional Address $testName")
        val additionalSiteAddressResponseA = testDataClient.createAdditionalAddress(siteResponseA, "Additional Site Address $testName")

        val legalEntityResponseB = testDataClient.createParticipantLegalEntity("B $testName")
        val siteResponseB = testDataClient.createSite(legalEntityResponseB, "Site B $testName")
        testDataClient.createAdditionalAddress(siteResponseB, "Additional Address B $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(siteResponseA.site.bpns))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponse(siteResponseA),
            resultFactory.buildAddressSearchResponse(additionalSiteAddressResponseA)
        )
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for address by name
     * THEN sharing member sees only addresses with this name
     */
    @Test
    fun `search address by name`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSite(legalEntityResponse, "Legal Site $testName")
        val siteResponse = testDataClient.createSite(legalEntityResponse, "Site $testName")
        testDataClient.createAdditionalAddress(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(name = legalAddressSiteResponse.mainAddress.name)
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(resultFactory.buildAddressSearchResponseFromLegalSite(legalAddressSiteResponse, legalEntityResponse))
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for address page
     * THEN sharing member sees only addresses within that page
     */
    @Test
    fun `search addresses by page`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSite(legalEntityResponse, "Legal Site $testName")
        val siteResponse = testDataClient.createSite(legalEntityResponse, "Site $testName")
        testDataClient.createAdditionalAddress(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddress(siteResponse, "Additional Site Address $testName")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.addresses.getAddresses(AddressSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.addresses.searchAddresses(AddressSearchRequest(), paginationRequest)

        //THEN
        val expectedAddresses = listOf(
            resultFactory.buildAddressSearchResponseFromLegalSite(legalAddressSiteResponse, legalEntityResponse),
            resultFactory.buildAddressSearchResponse(siteResponse)
        )
        val expectedResponse = PageDto(4, 2, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }
}
