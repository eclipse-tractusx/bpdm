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

package org.eclipse.tractusx.bpdm.pool.v7.legalentity

import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.*
import org.junit.jupiter.api.Test

class LegalEntityCreationV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * WHEN operator creates a new valid non-participant legal entity
     * THEN created legal entity with no confidence is returned
     */
    @Test
    fun `create valid non-participant legal entity`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withParticipantData(false)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityCreate(legalEntityRequest).withConfidence(TestDataV7.NoConfidence))
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator creates a new valid participant legal entity
     * THEN created legal entity with shared by owner confidence returned
     */
    @Test
    fun `create valid participant legal entity`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withParticipantData(true)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityCreate(legalEntityRequest).withConfidence(TestDataV7.SharedByOwnerConfidence))
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal identifier X
     * WHEN operator tries to create a new legal entity with the same legal identifier X
     * THEN operator sees a LegalEntityDuplicateIdentifier error entry in response
     */
    @Test
    fun `create legal entity with duplicate identifier`(){
        //GIVEN
        val identifierX = requestFactory.buildLegalEntityIdentifier(testName)
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName 1").withLegalIdentifiers(identifierX))

        //WHEN
        val newLegalEntityRequest = requestFactory.buildLegalEntityCreateRequest("$testName 2").withLegalIdentifiers(identifierX)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address identifier X
     * WHEN operator tries to create a new legal entity with the same legal address identifier X
     * THEN operator sees a LegalAddressDuplicateIdentifier error entry in response
     */
    @Test
    fun `create legal entity with duplicate legal address identifier`(){
        //GIVEN
        val identifierX = requestFactory.buildAddressIdentifier(testName)
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName 1").withLegalAddressIdentifiers(identifierX))

        //WHEN
        val newLegalEntityRequest = requestFactory.buildLegalEntityCreateRequest("$testName 2").withLegalAddressIdentifiers(identifierX)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create two legal entities with the same legal identifier in the same request
     * THEN operator sees LegalEntityDuplicateIdentifier error entries for both legal entities in response
     */
    @Test
    fun `create two legal entities with same legal identifier in same request`(){
        //WHEN
        val sharedIdentifier = requestFactory.buildLegalEntityIdentifier(testName)
        val request1 = requestFactory.buildLegalEntityCreateRequest("$testName 1").withLegalIdentifiers(sharedIdentifier)
        val request2 = requestFactory.buildLegalEntityCreateRequest("$testName 2").withLegalIdentifiers(sharedIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(request1, request2))

        //THEN
        val expectedErrors = listOf(request1, request2).map { ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create two legal entities with the same legal address identifier in the same request
     * THEN operator sees LegalAddressDuplicateIdentifier error entries for both legal entities in response
     */
    @Test
    fun `create two legal entities with same legal address identifier in same request`(){
        //WHEN
        val sharedIdentifier = requestFactory.buildAddressIdentifier(testName)
        val request1 = requestFactory.buildLegalEntityCreateRequest("$testName 1").withLegalAddressIdentifiers(sharedIdentifier)
        val request2 = requestFactory.buildLegalEntityCreateRequest("$testName 2").withLegalAddressIdentifiers(sharedIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(request1, request2))

        //THEN
        val expectedErrors = listOf(request1, request2).map { ErrorInfo(LegalEntityCreateError.LegalAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal form
     * THEN operator sees LegalFormNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown legal form`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalForm("UNKNOWN")
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalFormNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown administrative region level 1 in physical address
     * THEN operator sees LegalAddressRegionNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown admin region in physical address`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withPhysicalAdminArea("UNKNOWN")
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressRegionNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown administrative region level 1 in alternative address
     * THEN operator sees LegalAddressRegionNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown admin region in alternative address`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withAlternativeAdminArea("UNKNOWN")
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressRegionNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal identifier type
     * THEN operator sees LegalEntityIdentifierNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown legal identifier type`(){
        //WHEN
        val unknownIdentifier = LegalEntityIdentifierDto(value = "Any Value", type = "UNKNOWN", issuingBody = null)
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityIdentifierNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal address identifier type
     * THEN operator sees LegalAddressIdentifierNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown legal address identifier type`(){
        //WHEN
        val unknownIdentifier = AddressIdentifierDto(value = "Any Value", type = "UNKNOWN")
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalAddressIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressIdentifierNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with too many legal identifiers
     * THEN operator sees LegalEntityIdentifiersTooMany error entry in response
     */
    @Test
    fun `create legal entity with too many legal identifiers`(){
        //WHEN
        val identifiers = (1..101).map { requestFactory.buildLegalEntityIdentifier(testName, it) }
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalIdentifiers(identifiers)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityIdentifiersTooMany, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with too many legal address identifiers
     * THEN operator sees LegalAddressIdentifiersTooMany error entry in response
     */
    @Test
    fun `create legal entity with too many legal address identifiers`(){
        //WHEN
        val identifiers = (1..101).map { requestFactory.buildAddressIdentifier(testName, it) }
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalAddressIdentifiers(identifiers)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressIdentifiersTooMany, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }
}