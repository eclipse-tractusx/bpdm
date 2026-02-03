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
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolV6Test
import org.junit.jupiter.api.Test

class AddressMemberSearchV6IT: UnscheduledPoolV6Test() {

    /**
     * GIVEN member and non-member address data
     * WHEN participant searches for member address data
     * THEN participant only sees member address data
     */
    @Test
    fun `search for member addresses`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        val memberAddress = testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        val memberSiteAddress =  testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchResponse = poolClient.members.searchAddresses(AddressSearchRequest(), PaginationRequest())

        //THEN
        val expectedAddresses = listOf(member1.legalAddress, member2.legalAddress, memberSite.mainAddress, memberAddress.address, memberSiteAddress.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member address data with BPNA
     * WHEN participant searches for member address data by that BPNA
     * THEN participant only sees member address data with that BPNA
     */
    @Test
    fun `search for member addresses by BPNA`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        val memberAddress = testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(addressBpns = listOf(memberAddress.address.bpna))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(memberAddress.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member address data with name
     * WHEN participant searches for member address data by that name
     * THEN participant only sees member address data with that name
     */
    @Test
    fun `search for member addresses by name`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        val memberAddress = testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(name = memberAddress.address.name)
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(memberAddress.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member address data belong to legal entity with BPNL
     * WHEN participant searches for member address data by that BPNL
     * THEN participant only sees member address data with that BPNL
     */
    @Test
    fun `search for member addresses by legal entity BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        val memberAddress = testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        val memberSiteAddress = testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(member1.legalEntity.bpnl))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(member1.legalAddress, memberSite.mainAddress, memberAddress.address, memberSiteAddress.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member address data belong to site with BPNS
     * WHEN participant searches for member address data by that BPNS
     * THEN participant only sees member address data with that BPNS
     */
    @Test
    fun `search for member addresses by site BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        val memberSiteAddress = testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(memberSite.site.bpns))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(memberSite.mainAddress, memberSiteAddress.address)
        val expectedResponse = PageDto(expectedAddresses.size.toLong(), 1, 0, expectedAddresses.size, expectedAddresses)

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member address data with BPNS
     * WHEN participant searches for member address data by that BPNS
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-member addresses by BPNS`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(nonMember.legalAddress.bpna))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member address data with name
     * WHEN participant searches for member address data by that name
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-member addresses by name`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(name = nonMember.legalAddress.name)
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member address data belonging to legal entity with BPNL
     * WHEN participant searches for member address data by that BPNL
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-member addresses by legal entity BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")

        testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(name = nonMember.legalAddress.bpnLegalEntity)
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member address data belonging to site with BPNS
     * WHEN participant searches for member address data by that BPNS
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-member addresses by site BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite = testDataClient.createSiteFor(member1, "$testName Member Site")
        val nonMemberSite = testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        testDataClient.createAdditionalAddressFor(member1, "$testName Member Address")
        testDataClient.createAdditionalAddressFor(memberSite, "$testName Member Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(nonMemberSite.site.bpns))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertAddressSearch(searchResponse, expectedResponse)
    }
}