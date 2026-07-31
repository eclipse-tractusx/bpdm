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
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.*
import org.junit.jupiter.api.Test

class AddressUpdateV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN participant legal entity
     * WHEN owner updates legal address with valid values
     * THEN updated address with is shared by owner confidence is returned
     */
    @Test
    fun `update valid legal address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse).withConfidence(TestDataV7.SharedByOwner)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildLegalAddressUpdate(addressRequest, legalEntityResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN site
     * WHEN operator updates site main address with valid values
     * THEN updated address with is shared by owner confidence is returned
     */
    @Test
    fun `update valid site main address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, siteResponse).withConfidence(TestDataV7.SharedByOwner)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildSiteMainAddressUpdate(addressRequest, siteResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN legal address site
     * WHEN operator updates site legal main address with valid values
     * THEN updated address with is shared by owner confidence is returned
     */
    @Test
    fun `update valid legal site main address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createLegalAddressSite(legalEntityResponse, testName)

        //WHEN
        // The site main address is the legal address, so the update has to keep covering the legal entity's script codes.
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, siteResponse)
            .copy(scriptVariants = requestFactory.buildAddressScriptVariants(legalEntityResponse.scriptVariants.map { it.scriptCode }, testName))
            .withConfidence(TestDataV7.SharedByOwner)
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedAddress = resultFactory.buildSiteMainAddressUpdate(addressRequest, siteResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN additional address
     * WHEN owner updates additional address with valid values
     * THEN updated address with is shared by owner confidence is returned
     */
    @Test
    fun `update valid additional address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val addressCreateResponse = testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        //WHEN
        val addressUpdateRequest = requestFactory.buildAddressUpdateRequest("Updated $testName", addressCreateResponse).withConfidence(TestDataV7.SharedByOwner)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressUpdate(addressUpdateRequest, addressCreateResponse).withConfidence(TestDataV7.SharedByOwnerConfidence)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressUpdateResponse, expectedResponse)
    }

    /**
     * GIVEN additional site address
     * WHEN operator updates additional site address with valid values
     * THEN updated address information is returned
     */
    @Test
    fun `update valid additional site address`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val siteResponse = testDataClient.createSite(legalEntityResponse, testName)
        val addressCreateResponse = testDataClient.createAdditionalAddress(siteResponse, testName)

        //WHEN
        val addressUpdateRequest = requestFactory.buildAddressUpdateRequest("Updated $testName", addressCreateResponse)
        val addressUpdateResponse = poolClient.addresses.updateAddresses(listOf(addressUpdateRequest))

        //THEN
        val expectedAddress = resultFactory.buildAdditionalAddressUpdate(addressUpdateRequest, addressCreateResponse)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(listOf(expectedAddress), emptyList())

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressUpdateResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to update address with an unknown BPN
     * THEN operator sees address not found error
     */
    @Test
    fun `try update address with unknown bpn`() {
        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, "UNKNOWN")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressNotFound, "IGNORED", "UNKNOWN")
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address 1 with identifier X and address 2
     * WHEN operator tries to update address 2 with same identifier X
     * THEN operator sees duplicate identifier error
     */
    @Test
    fun `try update address with duplicate identifier`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)
        val identifierX = legalEntityResponse.legalAddress.identifiers.first()

        val addressCreateResponse = testDataClient.createAdditionalAddress(legalEntityResponse, testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, addressCreateResponse)
            .withIdentifiers(AddressIdentifierDto(identifierX.value, identifierX.typeVerbose.technicalKey))
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.AddressDuplicateIdentifier, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown physical region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown physical region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withPhysicalAdminArea("UNKNOWN")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown alternative region
     * THEN operator sees region not found error
     */
    @Test
    fun `try update address with unknown alternative region`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withAlternativeAdminArea("UNKNOWN")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.RegionNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with unknown identifier type
     * THEN operator sees identifier type not found error
     */
    @Test
    fun `try update address with unknown identifier type`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse).let { req ->
            val unknownIdentifier = req.address.identifiers.first().copy(type = "UNKNOWN")
            req.withIdentifiers(req.address.identifiers.drop(1).plus(unknownIdentifier))
        }
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifierNotFound, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with too many identifiers
     * THEN operator sees too many identifiers error
     */
    @Test
    fun `try update address with too many identifiers`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withIdentifiers((1..101).map { requestFactory.buildAddressIdentifier(testName, it) })
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.IdentifiersTooMany, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with a script variant whose physical city is blank
     * THEN operator sees ScriptVariantCityMissing error
     */
    @Test
    fun `try update address with script variant with blank physical city`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withScriptVariantPhysicalCity("  ")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.ScriptVariantCityMissing, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with a script variant whose alternative address city is blank
     * THEN operator sees ScriptVariantCityMissing error
     */
    @Test
    fun `try update address with script variant with blank alternative city`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withScriptVariantAlternativeCity("  ")
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.ScriptVariantCityMissing, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN address
     * WHEN operator tries to update the address with two script variants of the same script code
     * THEN operator sees ScriptVariantDuplicateScriptCode error
     */
    @Test
    fun `try update address with duplicate script codes`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse)
            .withDuplicateScriptVariants()
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedError = ErrorInfo(AddressUpdateError.ScriptVariantDuplicateScriptCode, "IGNORED", addressRequest.bpna)
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }

    /**
     * GIVEN participant legal entity with a script variant
     * WHEN operator tries to update its legal address without the script variant the legal entity is named in
     * THEN operator sees ScriptVariantCoverageStillNeeded error
     */
    @Test
    fun `try update legal address dropping a script variant its legal entity is named in`() {
        //GIVEN
        val legalEntityResponse = testDataClient.createParticipantLegalEntity(testName)

        //WHEN
        val addressRequest = requestFactory.buildAddressUpdateRequest(testName, legalEntityResponse).copy(scriptVariants = emptyList())
        val addressResponse = poolClient.addresses.updateAddresses(listOf(addressRequest))

        //THEN
        val expectedErrors = legalEntityResponse.scriptVariants
            .map { ErrorInfo(AddressUpdateError.ScriptVariantCoverageStillNeeded, "IGNORED", addressRequest.bpna) }
        val expectedResponse = AddressPartnerUpdateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertAddressUpdateResponseWrapperIsEqual(addressResponse, expectedResponse)
    }
}
