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

package org.eclipse.tractusx.bpdm.pool.v6.address

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolV6Test
import org.junit.jupiter.api.Test

class AddressSearchV6IT: UnscheduledPoolV6Test() {

    /**
     * GIVEN addresses
     * WHEN sharing member searches for all addresses
     * THEN sharing member sees all addresses
     */
    @Test
    fun `search addresses`(){
        //GIVEN
        val legalEntityResponse =  testDataClient.createLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSiteFor(legalEntityResponse, "Legal Site $testName") //Should not create new main address
        val siteResponse =  testDataClient.createSiteFor(legalEntityResponse, "Site $testName")
        val additionalAddressResponse = testDataClient.createAdditionalAddressFor(legalEntityResponse, "Additional Address $testName")
        val additionalSiteAddressResponse = testDataClient.createAdditionalAddressFor(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchResponseGet = poolClient.addresses.getAddresses(AddressSearchRequest(), PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(AddressSearchRequest(), PaginationRequest())

        //THEN
        val expectedAddresses = listOf(legalAddressSiteResponse.mainAddress, siteResponse.mainAddress, additionalAddressResponse.address, additionalSiteAddressResponse.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by BPNA
     * THEN sharing member sees only addreses with these BPNAs
     */
    @Test
    fun `search addresses by BPNA`(){
        //GIVEN
        val legalEntityResponse =  testDataClient.createLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSiteFor(legalEntityResponse, "Legal Site $testName") //Should not create new main address
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, "Site $testName")
        val additionalAddressResponse = testDataClient.createAdditionalAddressFor(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddressFor(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(addressBpns = listOf(legalAddressSiteResponse.mainAddress.bpna, additionalAddressResponse.address.bpna))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(legalAddressSiteResponse.mainAddress, additionalAddressResponse.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by their legal entity BPNLs
     * THEN sharing member sees only addresses from these legal entities
     */
    @Test
    fun `search addresses by BPNLs`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("A $testName")
        val legalAddressSiteResponseA = testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "Legal Site $testName") //Should not create new main address
        val siteResponseA =  testDataClient.createSiteFor(legalEntityResponseA, "Site $testName")
        val additionalAddressResponseA = testDataClient.createAdditionalAddressFor(legalEntityResponseA, "Additional Address $testName")
        val additionalSiteAddressResponseA = testDataClient.createAdditionalAddressFor(siteResponseA, "Additional Site Address $testName")

        val legalEntityResponseB =  testDataClient.createLegalEntity("B $testName")
        val siteResponseB =  testDataClient.createSiteFor(legalEntityResponseB, "Site B $testName")
        testDataClient.createAdditionalAddressFor(siteResponseB, "Additional Address B $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(legalEntityResponseA.legalEntity.bpnl))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(legalAddressSiteResponseA.mainAddress, siteResponseA.mainAddress, additionalAddressResponseA.address, additionalSiteAddressResponseA.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for addresses by their site BPNS
     * THEN sharing member sees only addresses from these sites
     */
    @Test
    fun `search addresses by BPNS`(){
        //GIVEN
        val legalEntityResponseA =  testDataClient.createLegalEntity("A $testName")
        testDataClient.createLegalAddressSiteFor(legalEntityResponseA, "Legal Site $testName") //Should not create new main address
        val siteResponseA =  testDataClient.createSiteFor(legalEntityResponseA, "Site $testName")
        testDataClient.createAdditionalAddressFor(legalEntityResponseA, "Additional Address $testName")
        val additionalSiteAddressResponseA = testDataClient.createAdditionalAddressFor(siteResponseA, "Additional Site Address $testName")

        val legalEntityResponseB =  testDataClient.createLegalEntity("B $testName")
        val siteResponseB =  testDataClient.createSiteFor(legalEntityResponseB, "Site B $testName")
        testDataClient.createAdditionalAddressFor(siteResponseB, "Additional Address B $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(siteResponseA.site.bpns))
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(siteResponseA.mainAddress, additionalSiteAddressResponseA.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for address by name
     * THEN sharing member sees only addresses with this name
     */
    @Test
    fun `search address by name`(){
        //GIVEN
        val legalEntityResponse =  testDataClient.createLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSiteFor(legalEntityResponse, "Legal Site $testName") //Should not create new main address
        val siteResponse =  testDataClient.createSiteFor(legalEntityResponse, "Site $testName")
        testDataClient.createAdditionalAddressFor(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddressFor(siteResponse, "Additional Site Address $testName")

        //WHEN
        val searchRequest = AddressSearchRequest(name = legalAddressSiteResponse.mainAddress.name)
        val searchResponseGet = poolClient.addresses.getAddresses(searchRequest, PaginationRequest())
        val searchResponsePost = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(legalAddressSiteResponse.mainAddress)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }

    /**
     * GIVEN addresses
     * WHEN sharing member searches for address page
     * THEN sharing member sees only addresses within that page
     */
    @Test
    fun `search addresses by page`(){
        //GIVEN
        val legalEntityResponse =  testDataClient.createLegalEntity(testName)
        val legalAddressSiteResponse = testDataClient.createLegalAddressSiteFor(legalEntityResponse, "Legal Site $testName") //Should not create new main address
        val siteResponse =  testDataClient.createSiteFor(legalEntityResponse, "Site $testName")
        testDataClient.createAdditionalAddressFor(legalEntityResponse, "Additional Address $testName")
        testDataClient.createAdditionalAddressFor(siteResponse, "Additional Site Address $testName")

        //WHEN
        val paginationRequest = PaginationRequest(0, 2)
        val searchResponseGet = poolClient.addresses.getAddresses(AddressSearchRequest(), paginationRequest)
        val searchResponsePost = poolClient.addresses.searchAddresses(AddressSearchRequest(), paginationRequest)

        //THEN
        val expectedAddresses = listOf(legalAddressSiteResponse.mainAddress, siteResponse.mainAddress)
        val expectedResponse = PageDto(4, 2, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponseGet, expectedResponse)
        assertRepository.assertAddressSearch(searchResponsePost, expectedResponse)
    }


}