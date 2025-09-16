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

package org.eclipse.tractusx.bpdm.pool.v6.operator.legalentity

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LegalEntityUpdateIT @Autowired constructor(
    private val poolClient: PoolApiClient,
    private val testDataV6Factory: TestDataV6Factory,
    private val assertRepo: AssertRepositoryV6
): OperatorTest() {

    /**
     * GIVEN legal entity
     * WHEN operator updates the legal entity
     * THEN updated legal entity returned
     */
    @Test
    fun `update legal entity`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedLegalEntities = response.entities.map { testDataV6Factory.result.mapToExpectedLegalEntityUpdate(updateRequest) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN operator updated legal entity
     * WHEN operator searches for legal entity
     * THEN operator can find updated legal entity
     */
    @Test
    fun `update legal entity and find it`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity
        val updateRequest = testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //WHEN
        val response = poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(bpnLs = listOf(givenLegalEntity.bpnl)), PaginationRequest())

        //THEN
        val expectedLegalEntities = updateResponse.entities.map { testDataV6Factory.result.mapToExpectedLegalEntity(it) }
        val expectedResponse = PageDto(1, 1, 0, 1, expectedLegalEntities)

        assertRepo.assertLegalEntityGet(response, expectedResponse)
    }

    /**
     * GIVEN legal entity A with identifier X and legal entity B
     * WHEN operator tries to update legal entity B with identifier X
     * THEN operator sees duplicate identifier error entry in response
     */
    @Test
    fun `try update legal entity with duplicated identifier`(){
        //GIVEN
        val legalEntityRequestA = testDataV6Factory.request.createLegalEntityRequest("$testName A")
        val identifierX = legalEntityRequestA.legalEntity.identifiers.first()
        poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequestA))

        val legalEntityRequestB = testDataV6Factory.request.createLegalEntityRequest("$testName B")
        val legalEntityB = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequestB)).entities.single()

        //WHEN
        val updateRequest =  with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", legalEntityB.legalEntity.bpnl)){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(identifierX)))
        }
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityUpdate(updateResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity A and legal entity B
     * WHEN operator tries to update legal entity A and B both with same identifier
     * THEN operator sees duplicate identifier error entries in response
     */
    @Test
    fun `try update legal entity having duplicated identifiers`(){
        //GIVEN
        val legalEntityRequestA = testDataV6Factory.request.createLegalEntityRequest("$testName A")
        val legalEntityA = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequestA)).entities.single()

        val legalEntityRequestB = testDataV6Factory.request.createLegalEntityRequest("$testName B")
        val legalEntityB = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequestB)).entities.single()

        //WHEN
        val updateRequestA = testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName A", legalEntityA.legalEntity.bpnl)
        val sameIdentifier = updateRequestA.legalEntity.identifiers.first()
        val updateRequestB =  with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName B", legalEntityB.legalEntity.bpnl)){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(sameIdentifier)))
        }
        val updateResponse = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequestA, updateRequestB))

        //THEN
        val expectedErrors = listOf(updateRequestA, updateRequestB).map {  ErrorInfo(LegalEntityUpdateError.LegalEntityDuplicateIdentifier, "IGNORED", it.bpnl) }
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), expectedErrors)
        assertRepo.assertLegalEntityUpdate(updateResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to update the legal entity with an unknown BPNL
     * THEN operator sees legal entity not found error in response
     */
    @Test
    fun `try update legal entity with unknown BPNL`(){
        //WHEN
        val updateRequest = testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", "UNKNOWN")
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal form
     * THEN operator sees legal form not found error in response
     */
    @Test
    fun `try update legal entity with unknown legal form`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            copy(legalEntity = legalEntity.copy(legalForm = "UNKNOWN"))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalFormNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown physical region
     * THEN operator sees region not found error in response
     */
    @Test
    fun `try update legal entity with unknown physical region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            copy(legalAddress = legalAddress.copy(physicalPostalAddress = legalAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN" )))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown physical region
     * THEN operator sees region not found error in response
     */
    @Test
    fun `try update legal entity with unknown alternative region`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            copy(legalAddress = legalAddress.copy(alternativePostalAddress = legalAddress.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN" )))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressRegionNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown identifier type
     * THEN operator sees identifier type not found error in response
     */
    @Test
    fun `try update legal entity with unknown identifier type`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            val unknownIdentifier = legalEntity.identifiers.first().copy(type = "UNKNOWN")
            copy(legalEntity = legalEntity.copy(identifiers = legalEntity.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with an unknown legal address identifier type
     * THEN operator sees legal address identifier type not found error in response
     */
    @Test
    fun `try update legal entity with unknown legal address identifier type`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            val unknownIdentifier = legalAddress.identifiers.first().copy(type = "UNKNOWN")
            copy(legalAddress = legalAddress.copy(identifiers = legalAddress.identifiers.drop(1).plus(unknownIdentifier)))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalAddressIdentifierNotFound, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }


    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many identifiers
     * THEN operator sees too many identifiers error in response
     *
     * ToDo:
     * At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try update legal entity with too many identifiers`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            copy(legalEntity = legalEntity.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createLegalEntityIdentifier("Updated $testName", it) }))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifiersTooMany, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }

    /**
     * GIVEN legal entity
     * WHEN operator tries to update the legal entity with too many legal address identifiers
     * THEN operator sees too many legal address identifiers error in response
     *
     * ToDo:
     * At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try update legal entity with too many legal address identifiers`(){
        //GIVEN
        val legalEntityRequest = testDataV6Factory.request.createLegalEntityRequest("Original $testName")
        val givenLegalEntity = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest)).entities.single().legalEntity

        //WHEN
        val updateRequest = with(testDataV6Factory.request.createLegalEntityUpdateRequest("Updated $testName", givenLegalEntity.bpnl)){
            copy(legalAddress = legalAddress.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createAddressIdentifier("Updated $testName", it) }))
        }
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(updateRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityUpdateError.LegalEntityIdentifiersTooMany, "IGNORED", updateRequest.bpnl)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapper(emptyList(), listOf(expectedError))

        assertRepo.assertLegalEntityUpdate(response, expectedResponse)
    }
}