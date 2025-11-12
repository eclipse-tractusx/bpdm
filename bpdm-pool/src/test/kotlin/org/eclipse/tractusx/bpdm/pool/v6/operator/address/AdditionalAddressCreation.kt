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
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.controller.v6.LegalEntityLegacyServiceMapper.Companion.IDENTIFIER_AMOUNT_LIMIT
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.junit.jupiter.api.Test

class AdditionalAddressCreation: OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator creates a new valid additional address for the legal entity
     * THEN created address returned
     */
    @Test
    fun `create valid additional address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = testDataFactory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedAdditionalAddressCreateResponse(addressRequest, legalEntityResponse)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator creates a new valid additional address for the site
     * THEN created address returned
     */
    @Test
    fun `create valid additional site address`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val siteResponse = testDataClient.createSiteFor(legalEntityResponse, testName)

        //WHEN
        val addressRequest = testDataFactory.request.buildAdditionalAddressCreateRequest(testName, siteResponse.site.bpns)
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataFactory.result.buildExpectedAdditionalAddressCreateResponse(addressRequest, siteResponse)
        val expectedResponse = AddressPartnerCreateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an invalid BPN
     * THEN operator sees BPN invalid error
     */
    @Test
    fun `try create additional address for invalid bpn`(){
        //WHEN
        val addressRequest = testDataFactory.request.buildAdditionalAddressCreateRequest(testName, "INVALID")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.BpnNotValid, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an unknown legal entity
     * THEN operator sees legal entity not found error
     */
    @Test
    fun `try create additional address for unknown legal entity`(){
        //WHEN
        val addressRequest = testDataFactory.request.buildAdditionalAddressCreateRequest(testName, "BPNLUnknown")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.LegalEntityNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new additional address for an unknown site
     * THEN operator sees site not found error
     */
    @Test
    fun `try create additional address for unknown site`(){
        //WHEN
        val addressRequest = testDataFactory.request.buildAdditionalAddressCreateRequest(testName, "BPNSUnknown")
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressCreateError.SiteNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address identifier X
     * WHEN operator tries to create a new additional address having the same identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create additional address with duplicate identifier`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val identifierX = legalEntityResponse.legalAddress.identifiers.first()

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(identifiers = listOf(AddressIdentifierDto(identifierX.value, identifierX.type))))
        }
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))


        //THEN
        val expectedError = ErrorInfo(AddressCreateError.AddressDuplicateIdentifier, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create two new additional addresses having the same identifier
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try create additional addresses having duplicate identifier`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest1 = testDataFactory.request.buildAdditionalAddressCreateRequest("Address 1 $testName", legalEntityResponse)
        val sameIdentifier = addressRequest1.address.identifiers.first()

        val addressRequest2 = with(testDataFactory.request.buildAdditionalAddressCreateRequest("Address 2 $testName", legalEntityResponse)){
            copy(address = address.copy(identifiers = listOf(sameIdentifier)))
        }
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest1, addressRequest2))

        //THEN
        val expectedErrors = listOf(addressRequest1, addressRequest2).map { ErrorInfo(AddressCreateError.AddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create additional address with unknown physical region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))


        //THEN
        val expectedError = ErrorInfo(AddressCreateError.RegionNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try create additional address with unknown alternative region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))


        //THEN
        val expectedError = ErrorInfo(AddressCreateError.RegionNotFound, "IGNORED", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to create a new additional address with too many identifiers
     * THEN operator sees too many identifiers error
     */
    @Test
    fun `try create additional address with too many identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val addressRequest = with(testDataFactory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse)){
            copy(address = address.copy(identifiers = (1 .. 101).map { testDataFactory.request.createAddressIdentifier(testName, it) } ))
        }
        val addressResponse = poolClient.addresses.createAddresses(listOf(addressRequest))


        //THEN
        val expectedError = ErrorInfo(AddressCreateError.IdentifiersTooMany, "Amount of identifiers (101) exceeds limit of $IDENTIFIER_AMOUNT_LIMIT", addressRequest.index)
        val expectedResponse = AddressPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAdditionalAddressCreate(addressResponse, expectedResponse)
    }

}