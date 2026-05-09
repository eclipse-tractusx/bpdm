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
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressInvariantVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class AddressParticipantSearchV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN participant and non-participant address data
     * WHEN participant searches for participant address data
     * THEN participant only sees participant address data
     */
    @Test
    fun `search for participant addresses`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        val participant2 = testDataClient.createParticipantLegalEntity("$testName Participant 2")
        testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")

        val participantAddress = testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        val participantSiteAddress = testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchResponse = poolClient.members.searchAddresses(AddressSearchRequest(), PaginationRequest())

        //THEN
        val expectedAddresses = listOf(participant1.legalAddress, participant2.legalAddress, participantSite.mainAddress, participantAddress.address, participantSiteAddress.address)
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant address data with BPNA
     * WHEN participant searches for participant address data by that BPNA
     * THEN participant only sees participant address data with that BPNA
     */
    @Test
    fun `search for participant addresses by BPNA`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")

        val participantAddress = testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(addressBpns = listOf(participantAddress.address.bpna))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(participantAddress.address)
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant address data with name
     * WHEN participant searches for participant address data by that name
     * THEN participant only sees participant address data with that name
     */
    @Test
    fun `search for participant addresses by name`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")

        val participantAddress = testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(name = participantAddress.address.name)
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(participantAddress.address)
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant address data belonging to legal entity with BPNL
     * WHEN participant searches for participant address data by that BPNL
     * THEN participant only sees participant address data with that BPNL
     */
    @Test
    fun `search for participant addresses by legal entity BPN`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")

        val participantAddress = testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        val participantSiteAddress = testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(participant1.header.bpnl))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(participant1.legalAddress, participantSite.mainAddress, participantAddress.address, participantSiteAddress.address)
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN participant address data belonging to site with BPNS
     * WHEN participant searches for participant address data by that BPNS
     * THEN participant only sees participant address data with that BPNS
     */
    @Test
    fun `search for participant addresses by site BPN`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")

        testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        val participantSiteAddress = testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(participantSite.site.bpns))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedAddresses = listOf(participantSite.mainAddress, participantSiteAddress.address)
        val expectedResponse = resultFactory.buildSinglePageResponse(expectedAddresses)

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant address data with BPNA
     * WHEN participant searches for participant address data by that BPNA
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-participant addresses by BPNA`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipant = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(testDataClient.createParticipantLegalEntity("$testName Participant Site Owner"), "$testName Participant Site")

        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(nonParticipant.legalAddress.bpna))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressInvariantVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant address data with name
     * WHEN participant searches for participant address data by that name
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-participant addresses by name`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipant = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(testDataClient.createParticipantLegalEntity("$testName Participant Site Owner"), "$testName Participant Site")
        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(name = nonParticipant.legalAddress.name)
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressInvariantVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant address data belonging to legal entity with BPNL
     * WHEN participant searches for participant address data by that BPNL
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-participant addresses by legal entity BPN`() {
        //GIVEN
        testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipant = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(testDataClient.createParticipantLegalEntity("$testName Participant Site Owner"), "$testName Participant Site")
        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(nonParticipant.header.bpnl))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressInvariantVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }

    /**
     * GIVEN non-participant address data belonging to site with BPNS
     * WHEN participant searches for participant address data by that BPNS
     * THEN participant does not find that address data
     */
    @Test
    fun `try search for non-participant addresses by site BPN`() {
        //GIVEN
        val participant1 = testDataClient.createParticipantLegalEntity("$testName Participant 1")
        testDataClient.createParticipantLegalEntity("$testName Participant 2")
        val nonParticipant = testDataClient.createLegalEntity("$testName Non-Participant")

        val participantSite = testDataClient.createSite(participant1, "$testName Participant Site")
        val nonParticipantSite = testDataClient.createSite(nonParticipant, "$testName Non-Participant Site")

        testDataClient.createAdditionalAddress(participant1, "$testName Participant Address")
        testDataClient.createAdditionalAddress(participantSite, "$testName Participant Site Address")

        //WHEN
        val searchRequest = AddressSearchRequest(siteBpns = listOf(nonParticipantSite.site.bpns))
        val searchResponse = poolClient.members.searchAddresses(searchRequest, PaginationRequest())

        //THEN
        val expectedResponse = PageDto<LogisticAddressInvariantVerboseDto>(0, 0, 0, 0, emptyList())

        assertRepository.assertParticipantAddressSearch(searchResponse, expectedResponse)
    }
}
