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

package org.eclipse.tractusx.bpdm.pool.v6.operator.address

import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AddressUpdate: OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator updates legal address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid legal address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = testDataFactory.request.buildAddressUpdateRequest(testName, legalEntityResponse)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedLegalAddressUpdateResponse(addressRequest, legalEntityResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator updates site main address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid site main address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val addressRequest = testDataFactory.request.buildAddressUpdateRequest(testName, siteResponse.mainAddress.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedMainAddressUpdateResponse(addressRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal address site
     * WHEN operator updates site legal main address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid legal site main address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createLegalAddressSiteFor(legalEntityResponse, testName)

        //WHEN
        val addressRequest = testDataFactory.request.buildAddressUpdateRequest(testName, siteResponse.mainAddress.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedMainAddressUpdateResponse(addressRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN additional address
     * WHEN operator updates additional address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid additional address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val addressCreateResponse = testDataClient.createAdditionalAddressFor(legalEntityResponse, testName)

        //WHEN
        val addressUpdateRequest = testDataFactory.request.buildAddressUpdateRequest("Updated $testName", addressCreateResponse.address.bpna)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedAdditionalAddressUpdateResponse(addressUpdateRequest, legalEntityResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdate(addressUpdateResponse, expectedResponse)
    }

    /**
     * GIVEN additional site address
     * WHEN operator updates additional site address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid additional site address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)
        val addressCreateResponse = testDataClient.createAdditionalAddressFor(siteResponse, testName)

        //WHEN
        val addressUpdateRequest = testDataFactory.request.buildAddressUpdateRequest("Updated $testName", addressCreateResponse)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedAdditionalAddressUpdateResponse(addressUpdateRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdate(addressUpdateResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to update address with an unknown BPN
     * THEN operator sees address not found error
     */
    @Test
    fun `try update address with invalid identifier`(){
        //WHEN
        val addressRequest = testDataFactory.request.buildAddressUpdateRequest(testName, "UNKNOWN")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address 1 with identifier X and address 2
     * WHEN operator tries to update the address 2 with same identifier X
     * THEN updated address information is returned
     */
    @Test
    fun `try update address with duplicate identifier`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val identifierX = legalEntityResponse.legalAddress.identifiers.first()

        val addressCreateResponse = testDataClient.createAdditionalAddressFor(legalEntityResponse, testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAddressUpdateRequest(testName, addressCreateResponse)){
            copy(address = address.copy(identifiers = listOf(AddressIdentifierDto(identifierX.value, identifierX.type))))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressDuplicateIdentifier, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown physical region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAddressUpdateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown alternative region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAddressUpdateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown identifier type
     * THEN operator sees identifier type not found error
     */
    @Test
    fun `try update address with unknown identifier type`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAddressUpdateRequest(testName, legalEntityResponse)){
            val unknownIdentifier = address.identifiers.first().copy(type = "UNKNOWN")
            copy(address = address.copy(identifiers = address.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifierNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with too many identifiers
     * THEN operator sees too many identifiers error
     *
     * ToDo:
     *  Does not work at the moment https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try update address with too many identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAddressUpdateRequest(testName, legalEntityResponse)){
            val tooManyIdentifiers = (1 .. 101).map { testDataFactory.request.createAddressIdentifier(testName, it) }
            copy(address = address.copy(identifiers = tooManyIdentifiers))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifiersTooMany, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdate(addressResponse, expectedResponse)
    }

}