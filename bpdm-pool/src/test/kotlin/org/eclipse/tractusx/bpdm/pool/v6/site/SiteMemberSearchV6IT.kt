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

package org.eclipse.tractusx.bpdm.pool.v6.site

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolV6Test
import org.junit.jupiter.api.Test

class SiteMemberSearchV6IT: UnscheduledPoolV6Test() {

    /**
     * GIVEN member and non-member site data
     * WHEN participant searches for member site data
     * THEN participant only sees member site data
     */
    @Test
    fun `search for member sites`(){
        //GIVEN
        val member = testDataClient.createMemberLegalEntity("$testName Member ")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite1 = testDataClient.createSiteFor(member, "$testName Member Site 1")
        val memberSite2 = testDataClient.createSiteFor(member, "$testName Member Site 2")
        testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchResponse = poolClient.members.postSiteSearch(SiteSearchRequest(), PaginationRequest())

        //THEN
        val expectedSites = listOf(memberSite1, memberSite2).map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }
        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member site data with BPNS
     * WHEN participant searches for member site data by BPNS
     * THEN participant only sees member site data with BPNS
     */
    @Test
    fun `search for member sites by BPNS`(){
        //GIVEN
        val member = testDataClient.createMemberLegalEntity("$testName Member ")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite1 = testDataClient.createSiteFor(member, "$testName Member Site 1")
        testDataClient.createSiteFor(member, "$testName Member Site 2")
        testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(memberSite1.site.bpns))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(memberSite1).map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }
        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member site data with name
     * WHEN participant searches for member site data by name
     * THEN participant only sees member site data with that name
     */
    @Test
    fun `search for member sites by name`(){
        //GIVEN
        val member = testDataClient.createMemberLegalEntity("$testName Member ")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite1 = testDataClient.createSiteFor(member, "$testName Member Site 1")
        testDataClient.createSiteFor(member, "$testName Member Site 2")
        testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(name = memberSite1.site.name)
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(memberSite1).map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }
        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN member site data belonging to legal entity with BPNL
     * WHEN participant searches for member site data by that legal entity BPN
     * THEN participant only sees member site data belonging to that legal entity
     */
    @Test
    fun `search for member sites by legal entity BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        val memberSite1 = testDataClient.createSiteFor(member1, "$testName Member Site 1")
        testDataClient.createSiteFor(member2, "$testName Member Site 2")
        testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(legalEntityBpns = listOf(memberSite1.site.bpnLegalEntity))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedSites = listOf(memberSite1).map { testDataFactory.result.buildExpectedSiteSearchResponse(it) }
        val expectedResponse = PageDto(expectedSites.size.toLong(), 1, 0, expectedSites.size, expectedSites)

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member site data with BPNS
     * WHEN participant searches for member site data by that BPNS
     * THEN participant does not find that non-member site data
     */
    @Test
    fun `try search for non-member sites by BPNS`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        testDataClient.createSiteFor(member1, "$testName Member Site 1")
        testDataClient.createSiteFor(member2, "$testName Member Site 2")
        val nonMemberSite = testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(siteBpns = listOf(nonMemberSite.site.bpnLegalEntity))
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<SiteWithMainAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member site data with name
     * WHEN participant searches for member site data by that name
     * THEN participant does not find that non-member site data
     */
    @Test
    fun `try search for non-member sites by name`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        testDataClient.createSiteFor(member1, "$testName Member Site 1")
        testDataClient.createSiteFor(member2, "$testName Member Site 2")
        val nonMemberSite = testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(name = nonMemberSite.site.name)
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<SiteWithMainAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-member site data belonging to legal entity with BPNL
     * WHEN participant searches for member site data by legal entity BPNL
     * THEN participant does not find that non-member site data
     */
    @Test
    fun `try search for non-member sites by legal entity BPN`(){
        //GIVEN
        val member1 = testDataClient.createMemberLegalEntity("$testName Member 1")
        val member2 = testDataClient.createMemberLegalEntity("$testName Member 2")
        val nonMember = testDataClient.createNonMemberLegalEntity("$testName Non-Member")

        testDataClient.createSiteFor(member1, "$testName Member Site 1")
        testDataClient.createSiteFor(member2, "$testName Member Site 2")
        val nonMemberSite = testDataClient.createSiteFor(nonMember, "$testName Non-Member Site")

        //WHEN
        val searchRequest = SiteSearchRequest(name = nonMemberSite.site.name)
        val searchResponse = poolClient.members.postSiteSearch(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<SiteWithMainAddressVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertSiteSearch(searchResponse, expectedResponse)
    }
}