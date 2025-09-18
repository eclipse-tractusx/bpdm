/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AddressUpdate @Autowired constructor(
    private val poolClient: PoolApiClient,
    private val testDataV6Factory: TestDataV6Factory,
    private val assertRepo: AssertRepositoryV6
): OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator updates legal address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid legal address`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val addressRequest = testDataV6Factory.request.buildAddressUpdateRequest(testName, legalEntityResponse.legalAddress.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataV6Factory.result.buildExpectedLegalAddressUpdateResponse(addressRequest, legalEntityResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator updates site main address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid site main address`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val siteRequest = testDataV6Factory.request.buildSiteCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSite(listOf(siteRequest)).entities.single()

        //WHEN
        val addressRequest = testDataV6Factory.request.buildAddressUpdateRequest(testName, siteResponse.mainAddress.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataV6Factory.result.buildExpectedMainAddressUpdateResponse(addressRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal address site
     * WHEN operator updates site legal main address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid legal site main address`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val siteRequest = testDataV6Factory.request.buildLegalAddressSiteCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest)).entities.single()

        //WHEN
        val addressRequest = testDataV6Factory.request.buildAddressUpdateRequest(testName, siteResponse.mainAddress.bpna)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = testDataV6Factory.result.buildExpectedMainAddressUpdateResponse(addressRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN additional address
     * WHEN operator updates additional address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid additional address`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val addressCreateRequest =  testDataV6Factory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val addressCreateResponse = poolClient.addresses.createAddresses(listOf(addressCreateRequest)).entities.single()

        //WHEN
        val addressUpdateRequest = testDataV6Factory.request.buildAddressUpdateRequest("Update $testName", addressCreateResponse.address.bpna)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = testDataV6Factory.result.buildExpectedAdditionalAddressUpdateResponse(addressUpdateRequest, legalEntityResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepo.assertAddressUpdate(addressUpdateResponse, expectedResponse)
    }

    /**
     * GIVEN additional site address
     * WHEN operator updates additional site address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid additional site address`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val siteRequest = testDataV6Factory.request.buildLegalAddressSiteCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val siteResponse = poolClient.sites.createSiteWithLegalReference(listOf(siteRequest)).entities.single()

        val addressCreateRequest =  testDataV6Factory.request.buildAdditionalAddressCreateRequest(testName, siteResponse.site.bpns)
        val addressCreateResponse = poolClient.addresses.createAddresses(listOf(addressCreateRequest)).entities.single()

        //WHEN
        val addressUpdateRequest = testDataV6Factory.request.buildAddressUpdateRequest("Update $testName", addressCreateResponse.address.bpna)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = testDataV6Factory.result.buildExpectedAdditionalAddressUpdateResponse(addressUpdateRequest, siteResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepo.assertAddressUpdate(addressUpdateResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to update address with an unknown BPN
     * THEN operator sees address not found error
     */
    @Test
    fun `try update address with invalid identifier`(){
        //WHEN
        val addressRequest = testDataV6Factory.request.buildAddressUpdateRequest(testName, "UNKNOWN")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address 1 with identifier X and address 2
     * WHEN operator tries to update the address 2 with same identifier X
     * THEN updated address information is returned
     */
    @Test
    fun `try update address with duplicate identifier`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        val identifierX = legalEntityRequest.legalAddress.identifiers.first()

        val addressCreateRequest =  testDataV6Factory.request.buildAdditionalAddressCreateRequest(testName, legalEntityResponse.legalEntity.bpnl)
        val addressCreateResponse = poolClient.addresses.createAddresses(listOf(addressCreateRequest)).entities.single()

        //WHEN
        val addressRequest = with(testDataV6Factory.request.buildAddressUpdateRequest(testName, addressCreateResponse.address.bpna)){
            copy(address = address.copy(identifiers = listOf(identifierX)))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressDuplicateIdentifier, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown physical region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val addressRequest = with(testDataV6Factory.request.buildAddressUpdateRequest(testName, legalEntityResponse.legalAddress.bpna)){
            copy(address = address.copy(physicalPostalAddress = address.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown alternative region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val addressRequest = with(testDataV6Factory.request.buildAddressUpdateRequest(testName, legalEntityResponse.legalAddress.bpna)){
            copy(address = address.copy(alternativePostalAddress = address.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown identifier type
     * THEN operator sees identifier type not found error
     */
    @Test
    fun `try update address with unknown identifier type`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val addressRequest = with(testDataV6Factory.request.buildAddressUpdateRequest(testName, legalEntityResponse.legalAddress.bpna)){
            val unknownIdentifier = address.identifiers.first().copy(type = "UNKNOWN")
            copy(address = address.copy(identifiers = address.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifierNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
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
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val legalEntityResponse = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single()

        //WHEN
        val addressRequest = with(testDataV6Factory.request.buildAddressUpdateRequest(testName, legalEntityResponse.legalAddress.bpna)){
            val tooManyIdentifiers = (1 .. 101).map { testDataV6Factory.request.createAddressIdentifier(testName, it) }
            copy(address = address.copy(identifiers = tooManyIdentifiers))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifiersTooMany, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertAddressUpdate(addressResponse, expectedResponse)
    }

}