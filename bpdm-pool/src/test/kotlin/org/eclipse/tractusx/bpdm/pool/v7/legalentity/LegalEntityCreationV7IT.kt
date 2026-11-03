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

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
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
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(expectedLegalEntities, emptyList())

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
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator creates a legal entity with a state that has neither start nor end date
     * THEN created legal entity reports that state from the earliest to the latest date
     */
    @Test
    fun `create legal entity with state without start and end date`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withParticipantData(false)
            .withStates(LegalEntityStateV7Dto(validFrom = null, validTo = null, type = BusinessStateType.ACTIVE))
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val openState = LegalEntityStateVerboseV7Dto(BusinessStateDates.EARLIEST, BusinessStateDates.LATEST, BusinessStateType.ACTIVE.toDto())
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityCreate(legalEntityRequest).withConfidence(TestDataV7.NoConfidence).withStates(openState))
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator creates a legal entity with a state starting before the earliest date and ending after the latest date
     * THEN created legal entity reports that state from the earliest to the latest date
     */
    @Test
    fun `create legal entity with state dates beyond the earliest and latest date`(){
        //WHEN
        val beforeEarliest = BusinessStateDates.EARLIEST.minusYears(100)
        val afterLatest = BusinessStateDates.LATEST.plusNanos(500_000_000)
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withParticipantData(false)
            .withStates(LegalEntityStateV7Dto(validFrom = beforeEarliest, validTo = afterLatest, type = BusinessStateType.ACTIVE))
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val openState = LegalEntityStateVerboseV7Dto(BusinessStateDates.EARLIEST, BusinessStateDates.LATEST, BusinessStateType.ACTIVE.toDto())
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityCreate(legalEntityRequest).withConfidence(TestDataV7.NoConfidence).withStates(openState))
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator creates a legal entity with a state starting on the latest date and ending on the earliest date
     * THEN created legal entity reports that state with exactly these dates
     */
    @Test
    fun `create legal entity with state starting on the latest and ending on the earliest date`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withParticipantData(false)
            .withStates(LegalEntityStateV7Dto(validFrom = BusinessStateDates.LATEST, validTo = BusinessStateDates.EARLIEST, type = BusinessStateType.ACTIVE))
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val reversedState = LegalEntityStateVerboseV7Dto(BusinessStateDates.LATEST, BusinessStateDates.EARLIEST, BusinessStateType.ACTIVE.toDto())
        val expectedLegalEntities = listOf(resultFactory.buildLegalEntityCreate(legalEntityRequest).withConfidence(TestDataV7.NoConfidence).withStates(reversedState))
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(expectedLegalEntities, emptyList())

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalEntityDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedErrors = listOf(request1, request2).map { ErrorInfoV7(LegalEntityCreateErrorV7.LegalEntityDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), expectedErrors)

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
        val expectedErrors = listOf(request1, request2).map { ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), expectedErrors)

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalFormNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressRegionNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressRegionNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal identifier type
     * THEN operator sees LegalEntityIdentifierNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown legal identifier type`(){
        //WHEN
        val unknownIdentifier = LegalEntityIdentifierV7Dto(value = "Any Value", type = "UNKNOWN", issuingBody = null)
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalEntityIdentifierNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal address identifier type
     * THEN operator sees LegalAddressIdentifierNotFound error entry in response
     */
    @Test
    fun `create legal entity with unknown legal address identifier type`(){
        //WHEN
        val unknownIdentifier = AddressIdentifierV7Dto(value = "Any Value", type = "UNKNOWN")
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalAddressIdentifiers(unknownIdentifier)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressIdentifierNotFound, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalEntityIdentifiersTooMany, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

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
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressIdentifiersTooMany, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with a script variant whose legal name is blank
     * THEN operator sees ScriptVariantLegalNameMissing error entry in response
     */
    @Test
    fun `create legal entity with script variant with blank legal name`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalForm(anyKnownLegalForm()).withScriptVariantLegalName("  ")
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.ScriptVariantLegalNameMissing, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with a script variant whose legal address city is blank
     * THEN operator sees LegalAddressScriptVariantCityMissing error entry in response
     */
    @Test
    fun `create legal entity with script variant with blank legal address city`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalForm(anyKnownLegalForm()).withScriptVariantPhysicalCity("  ")
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedError = ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressScriptVariantCityMissing, "IGNORED", legalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with two script variants of the same script code
     * THEN operator sees the duplicate reported for both the legal entity and its legal address
     */
    @Test
    fun `create legal entity with duplicate script codes`(){
        //WHEN
        val legalEntityRequest = requestFactory.buildLegalEntityCreateRequest(testName).withLegalForm(anyKnownLegalForm()).withDuplicateScriptVariants()
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        // One script variant carries both the legal name and the legal address in that script, so a duplicated script code
        // is reported by the legal entity and by the legal address.
        val expectedErrors = listOf(
            ErrorInfoV7(LegalEntityCreateErrorV7.ScriptVariantDuplicateScriptCode, "IGNORED", legalEntityRequest.index),
            ErrorInfoV7(LegalEntityCreateErrorV7.LegalAddressScriptVariantDuplicateScriptCode, "IGNORED", legalEntityRequest.index)
        )
        val expectedResponse = LegalEntityPartnerCreateV7ResponseWrapper(emptyList(), expectedErrors)

        assertRepository.assertLegalEntityCreateResponseWrapperIsEqual(response, expectedResponse)
    }

    // The v7 test metadata reads legal forms from the ELF code list, which is a superset of what the Pool's migration
    // inserts, so a seeded pick can land on a legal form the Pool does not know. Take one the Pool actually has.
    private fun anyKnownLegalForm(): String =
        poolClient.metadata.getLegalForms(PaginationRequest()).content.first().technicalKey
}
