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

package org.eclipse.tractusx.bpdm.pool.v6.operator.legalentity

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.controller.v6.LegalEntityLegacyServiceMapper.Companion.IDENTIFIER_AMOUNT_LIMIT
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.junit.jupiter.api.Test

class LegalEntityUpdateIT: OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator updates the legal entity
     * THEN updated legal entity returned
     */
    @Test
    fun `update legal entity`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedLegalEntities = response.entities.map { testDataFactory.result.buildExpectedLegalEntityUpdateResponse(updateRequest) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN operator updated legal entity
     * WHEN operator searches for legal entity
     * THEN operator can find updated legal entity
     */
    @Test
    fun `update legal entity and find it`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)
        val updateRequest = testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //WHEN
        val response = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(bpnLs = listOf(legalEntityResponse.legalEntity.bpnl)), PaginationRequest())

        //THEN
        val expectedLegalEntities = updateResponse.entities.map { testDataFactory.result.buildExpectedLegalEntitySearchResponse(it) }
        val expectedResponse = PageDto(1, 1, 0, 1, expectedLegalEntities)

        assertRepository.assertLegalEntitySearch(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A with identifier X and legal entity B
     * WHEN operator tries to update legal entity B with identifier X
     * THEN operator sees duplicate identifier error entry in response
     */
    @Test
    fun `try update legal entity with duplicated identifier`(){
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        val identifierX = legalEntityResponseA.legalEntity.identifiers.first()

        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        //WHEN
        val updateRequest =  with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponseB)){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(LegalEntityIdentifierDto(identifierX.value, identifierX.type, identifierX.issuingBody))))
        }
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepository.assertLegalEntityUpdate(updateResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity A and legal entity B
     * WHEN operator tries to update legal entity A and B both with same identifier
     * THEN operator sees duplicate identifier error entries in response
     */
    @Test
    fun `try update legal entity having duplicated identifiers`(){
        //GIVEN
        val legalEntityResponseA = testDataClient.createLegalEntity("$testName A")
        val legalEntityResponseB =  testDataClient.createLegalEntity("$testName B")

        //WHEN
        val updateRequestA = testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName A", legalEntityResponseA)
        val sameIdentifier = updateRequestA.legalEntity.identifiers.first()
        val updateRequestB =  with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName B", legalEntityResponseB)){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(sameIdentifier)))
        }
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequestA, updateRequestB))

        //THEN
        val expectedErrors = listOf(updateRequestA, updateRequestB).map {  ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", it.bpnl) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), expectedErrors)
        assertRepository.assertLegalEntityUpdate(updateResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to update the legal entity with an unknown BPNL
     * THEN operator sees legal entity not found error in response
     */
    @Test
    fun `try update legal entity with unknown BPNL`(){
        //WHEN
        val updateRequest = testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", "UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal form
     * THEN operator sees legal form not found error in response
     */
    @Test
    fun `try update legal entity with unknown legal form`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            copy(legalEntity = legalEntity.copy(legalForm = "UNKNOWN"))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalFormNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown physical region
     * THEN operator sees region not found error in response
     */
    @Test
    fun `try update legal entity with unknown physical region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            copy(legalAddress = legalAddress.copy(physicalPostalAddress = legalAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN" )))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown physical region
     * THEN operator sees region not found error in response
     */
    @Test
    fun `try update legal entity with unknown alternative region`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            copy(legalAddress = legalAddress.copy(alternativePostalAddress = legalAddress.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN" )))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown identifier type
     * THEN operator sees identifier type not found error in response
     */
    @Test
    fun `try update legal entity with unknown identifier type`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            val unknownIdentifier = legalEntity.identifiers.first().copy(type = "UNKNOWN")
            copy(legalEntity = legalEntity.copy(identifiers = legalEntity.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal address identifier type
     * THEN operator sees legal address identifier type not found error in response
     */
    @Test
    fun `try update legal entity with unknown legal address identifier type`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            val unknownIdentifier = legalAddress.identifiers.first().copy(type = "UNKNOWN")
            copy(legalAddress = legalAddress.copy(identifiers = legalAddress.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }


    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many identifiers
     * THEN operator sees too many identifiers error in response
     */
    @Test
    fun `try update legal entity with too many identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            copy(legalEntity = legalEntity.copy(identifiers = (1 .. 101).map { testDataFactory.request.createLegalEntityIdentifier("Updated $testName", it) }))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifiersTooMany, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many legal address identifiers
     * THEN operator sees too many legal address identifiers error in response
     */
    @Test
    fun `try update legal entity with too many legal address identifiers`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val updateRequest = with(testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityResponse)){
            copy(legalAddress = legalAddress.copy(identifiers = (1 .. 101).map { testDataFactory.request.createAddressIdentifier("Updated $testName", it) }))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressIdentifiersTooMany, "Amount of identifiers (101) exceeds limit of $IDENTIFIER_AMOUNT_LIMIT", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }
}