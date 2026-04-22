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

import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.*
import org.junit.jupiter.api.Test

class AdditionalAddressCreationV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN legal entity
     * WHEN owner creates a new valid additional address for the legal entity
     * THEN created address with shared by sharing member confidence is returned
     */
    @Test
    fun `create owner shared additional address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse).withConfidence(TestDataV7.IsShared)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressCreate(addressRequest, legalEntityResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator creates a new valid additional address for the legal entity
     * THEN created address with no confidence is returned
     */
    @Test
    fun `create not owned additional address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse).withConfidence(TestDataV7.NotCheckedNotOwned)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressCreate(addressRequest, legalEntityResponse).withConfidence(TestDataV7.NoConfidence)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN owner creates a new valid additional address for the site
     * THEN created address with shared by sharing member confidence returned
     */
    @Test
    fun `create owned additional site address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, siteResponse).withConfidence(TestDataV7.IsShared)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressCreate(addressRequest, siteResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator creates a not owned new valid additional address for the site
     * THEN created address with no confidence returned
     */
    @Test
    fun `create not owned additional site address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, siteResponse).withConfidence(TestDataV7.NotCheckedNotOwned)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressCreate(addressRequest, siteResponse).withConfidence(TestDataV7.NoConfidence)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an invalid BPN
     * THEN operator sees BPN invalid error
     */
    @Test
    fun `try create additional address for invalid bpn`() {
        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, "INVALID")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.BpnNotValid, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an unknown legal entity
     * THEN operator sees legal entity not found error
     */
    @Test
    fun `try create additional address for unknown legal entity`() {
        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, "BPNLUnknown")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.LegalEntityNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an unknown site
     * THEN operator sees site not found error
     */
    @Test
    fun `try create additional address for unknown site`() {
        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, "BPNSUnknown")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.SiteNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address identifier X
     * WHEN operator tries to create a new additional address having the same identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create additional address with duplicate identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val identifierX = legalEntityResponse.legalAddress.identifiers.first()

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)
            .withIdentifiers(AddressIdentifierDto(identifierX.value, identifierX.typeVerbose.technicalKey))
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.AddressDuplicateIdentifier, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create two new additional addresses having the same identifier
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create additional addresses having duplicate identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest1 = requestFactory.buildAdditionalAddressCreateRequest("Address 1 $testName", legalEntityResponse)
        val sameIdentifier = addressRequest1.address.identifiers.first()

        val addressRequest2 = requestFactory.buildAdditionalAddressCreateRequest("Address 2 $testName", legalEntityResponse)
            .withIdentifiers(sameIdentifier)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest1, addressRequest2))

        //THEN
        val expectedErrors = listOf(addressRequest1, addressRequest2).map { ErrorInfo(AddressCreateError.AddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create additional address with unknown physical region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)
            .withPhysicalAdminArea("UNKNOWN")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.RegionNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create additional address with unknown alternative region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)
            .withAlternativeAdminArea("UNKNOWN")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.RegionNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with too many identifiers
     * THEN operator sees too many identifiers error
     */
    @Test
    fun `try create additional address with too many identifiers`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)
            .withIdentifiers((1..101).map { requestFactory.buildAddressIdentifier(testName, it) })
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.IdentifiersTooMany, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressCreateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }
}
