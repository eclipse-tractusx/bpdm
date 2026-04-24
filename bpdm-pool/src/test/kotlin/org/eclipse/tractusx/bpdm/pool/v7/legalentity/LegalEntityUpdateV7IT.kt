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
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.*
import org.junit.jupiter.api.Test

class LegalEntityUpdateV7IT: UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN non-participant legal entity
     * WHEN operator updates the given legal entity as participant with new values
     * THEN legal entity with all values updated and shared by owner confidence is returned
     */
    @Test
    fun `update non-participant legal entity as participant`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName).withParticipantData(false))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withParticipantData(true)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityUpdate(updateRequest, givenLegalEntity).withConfidence(TestDataV7.SharedByOwnerConfidence))
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN non-participant legal entity
     * WHEN operator updates the given legal entity as non-participant with new values
     * THEN legal entity with all values updated and no confidence is returned
     */
    @Test
    fun `update participant legal entity as non-participant`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName).withParticipantData(true))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withParticipantData(false)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityUpdate(updateRequest, givenLegalEntity).withConfidence(TestDataV7.NoConfidence))
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A with identifier X and legal entity B
     * WHEN operator tries to update legal entity B with legal identifier X
     * THEN operator sees LegalEntityDuplicateIdentifier error entry in response
     */
    @Test
    fun `update legal entity with duplicate legal identifier`(){
        //GIVEN
        val identifierX = requestFactory.buildLegalEntityIdentifier(testName)
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName A").withLegalIdentifiers(identifierX))
        val legalEntityB = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName B"))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", legalEntityB.header.bpnl).withLegalIdentifiers(identifierX)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A with identifier X and legal entity B
     * WHEN operator tries to update legal entity B with legal address identifier X
     * THEN operator sees LegalEntityDuplicateIdentifier error entry in response
     */
    @Test
    fun `update legal entity with duplicate legal address identifier`(){
        //GIVEN
        val identifierX = requestFactory.buildAddressIdentifier(testName)
        testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName A").withLegalAddressIdentifiers(identifierX))
        val legalEntityB = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName B"))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", legalEntityB.header.bpnl).withLegalAddressIdentifiers(identifierX)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressDuplicateIdentifier, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A and legal entity B
     * WHEN operator tries to update legal entity A and B both with same legal identifier
     * THEN operator sees LegalEntityDuplicateIdentifier error entries in response
     */
    @Test
    fun `update two legal entities with same legal identifier in same request`(){
        //GIVEN
        val legalEntityA = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName A"))
        val legalEntityB = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName B"))

        //WHEN
        val sharedIdentifier = requestFactory.buildLegalEntityIdentifier(testName)
        val updateRequestA = requestFactory.buildLegalEntityUpdateRequest("Updated $testName A", legalEntityA.header.bpnl).withLegalIdentifiers(sharedIdentifier)
        val updateRequestB = requestFactory.buildLegalEntityUpdateRequest("Updated $testName B", legalEntityB.header.bpnl).withLegalIdentifiers(sharedIdentifier)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequestA, updateRequestB))

        //THEN
        val expectedErrors = listOf(updateRequestA, updateRequestB).map { ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", it.bpnl) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A and legal entity B
     * WHEN operator tries to update legal entity A and B both with same legal address identifier
     * THEN operator sees LegalEntityDuplicateIdentifier error entries in response
     */
    @Test
    fun `update two legal entities with same legal address identifier in same request`(){
        //GIVEN
        val legalEntityA = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName A"))
        val legalEntityB = testDataClient.createLegalEntity(requestFactory.buildLegalEntity("$testName B"))

        //WHEN
        val sharedIdentifier = requestFactory.buildAddressIdentifier(testName)
        val updateRequestA = requestFactory.buildLegalEntityUpdateRequest("Updated $testName A", legalEntityA.header.bpnl).withLegalAddressIdentifiers(sharedIdentifier)
        val updateRequestB = requestFactory.buildLegalEntityUpdateRequest("Updated $testName B", legalEntityB.header.bpnl).withLegalAddressIdentifiers(sharedIdentifier)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequestA, updateRequestB))

        //THEN
        val expectedErrors = listOf(updateRequestA, updateRequestB).map { ErrorInfo(LegalEntityUpdateError.LegalAddressDuplicateIdentifier, "IGNORED", it.bpnl) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to update legal entity with an unknown BPNL
     * THEN operator sees LegalEntityNotFound error in response
     */
    @Test
    fun `update legal entity with unknown BPNL`(){
        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest(testName, "UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal form
     * THEN operator sees LegalFormNotFound error in response
     */
    @Test
    fun `update legal entity with unknown legal form`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withLegalForm("UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalFormNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown physical region
     * THEN operator sees LegalAddressRegionNotFound error in response
     */
    @Test
    fun `update legal entity with unknown physical region`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withPhysicalAdminArea("UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown alternative region
     * THEN operator sees LegalAddressRegionNotFound error in response
     */
    @Test
    fun `update legal entity with unknown alternative region`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withAlternativeAdminArea("UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal identifier type
     * THEN operator sees LegalEntityIdentifierNotFound error in response
     */
    @Test
    fun `update legal entity with unknown legal identifier type`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val unknownIdentifier = LegalEntityIdentifierDto(value = "Any Value", type = "UNKNOWN", issuingBody = null)
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withLegalIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal address identifier type
     * THEN operator sees LegalAddressIdentifierNotFound error in response
     */
    @Test
    fun `update legal entity with unknown legal address identifier type`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val unknownIdentifier = AddressIdentifierDto(value = "Any Value", type = "UNKNOWN")
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withLegalAddressIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many identifiers
     * THEN operator sees LegalEntityIdentifiersTooMany error in response
     */
    @Test
    fun `update legal entity with too many legal identifiers`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val identifiers = (1..101).map { requestFactory.buildLegalEntityIdentifier(testName, it) }
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withLegalIdentifiers(identifiers)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifiersTooMany, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many legal address identifiers
     * THEN operator sees LegalAddressIdentifiersTooMany error in response
     */
    @Test
    fun `update legal entity with too many legal address identifiers`(){
        //GIVEN
        val givenLegalEntity = testDataClient.createLegalEntity(requestFactory.buildLegalEntity(testName))

        //WHEN
        val identifiers = (1..101).map { requestFactory.buildAddressIdentifier(testName, it) }
        val updateRequest = requestFactory.buildLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.header.bpnl).withLegalAddressIdentifiers(identifiers)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressIdentifiersTooMany, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdateResponseWrapperIsEqual(response, expectedResponse)
    }
}