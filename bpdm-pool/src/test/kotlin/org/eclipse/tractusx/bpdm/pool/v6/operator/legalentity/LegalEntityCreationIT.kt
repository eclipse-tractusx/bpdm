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
import org.eclipse.tractusx.bpdm.pool.api.model.AddressIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.pool.v6.util.TestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
class LegalEntityCreationIT @Autowired constructor(
    private val poolClient: PoolApiClient,
    private val testDataV6Factory: TestDataV6Factory,
    private val assertRepo: AssertRepositoryV6,
    private val testDataClient: TestDataClientV6
): OperatorTest() {

    /**
     * WHEN operator creates a new valid legal entity
     * THEN created legal entity returned
     */
    @Test
    fun `create valid legal entity`(){
        //WHEN
        val legalEntityRequest = testDataV6Factory.request.buildLegalEntityCreateRequest(testName)
        val response = poolClient.legalEntities.createBusinessPartners(listOf(legalEntityRequest))

        //THEN
        val expectedLegalEntities = response.entities.map { testDataV6Factory.result.buildExpectedLegalEntityCreateResponse(legalEntityRequest) }
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(expectedLegalEntities, emptyList())

        assertRepo.assertLegalEntityCreate(response, expectedResponse)
    }

    /**
     * GIVEN operator created legal entity
     * WHEN operator searches for legal entity by BPNL
     * THEN operator can find created legal entity
     */
    @Test
    fun `create valid legal entity and find it`(){
        //GIVEN
        val legalEntityResponse = testDataClient.createLegalEntity(testName)

        //WHEN
        val searchRequest = LegalEntitySearchRequest(listOf(legalEntityResponse.legalEntity.bpnl), null)
        val searchResponse = poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest())

        //THEN
        val expectedSearchResponse = createSingleResultPage(testDataV6Factory.result.buildExpectedLegalEntitySearchResponse(legalEntityResponse))
        assertRepo.assertLegalEntitySearch(searchResponse, expectedSearchResponse)
    }

    /**
     * GIVEN legal entity with identifier X
     * WHEN operator tries to create a new legal entity with identifier X
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with duplicated identifier`(){
        //GIVEN
        val givenLegalEntityResponse = testDataClient.createLegalEntity("$testName 1")
        val identifierX = givenLegalEntityResponse.legalEntity.identifiers.first()

        //WHEN
        val newLegalEntityRequest =  with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 2")){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(LegalEntityIdentifierDto(identifierX.value, identifierX.type, identifierX.issuingBody))))
        }
        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create two legal entities with the same identifier at the same time
     * THEN operator sees error entries for both legal entities in response
     */
    @Test
    fun `try create legal entities having duplicated identifiers`(){
        //WHEN
        val request1 = testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")
        val sameIdentifier = request1.legalEntity.identifiers.first()
        val request2 = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 2")){
            this.copy(legalEntity = legalEntity.copy(identifiers = listOf(sameIdentifier)))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(request1, request2))

        //THEN
        val expectedErrors = listOf(request1, request2).map { ErrorInfo(LegalEntityCreateError.LegalEntityDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * GIVEN legal entity with legal address identifier X
     * WHEN operator tries to create a new legal entity with legal address identifier X
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with duplicated legal address identifier`(){
        //GIVEN
        val givenLegalEntityResponse = testDataClient.createLegalEntity("$testName 1")
        val identifierX = givenLegalEntityResponse.legalAddress.identifiers.first()

        //WHEN
        val newLegalEntityRequest =  with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 2")){
            this.copy(legalAddress = legalAddress.copy(identifiers = listOf(AddressIdentifierDto(identifierX.value, identifierX.type))))
        }
        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressDuplicateIdentifier, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create two legal entities with the same legal address identifier at the same time
     * THEN operator sees error entries for both legal entities in response
     */
    @Test
    fun `try create legal entities having duplicated legal address identifiers`(){
        //WHEN
        val request1 = testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")
        val sameIdentifier = request1.legalAddress.identifiers.first()
        val request2 = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 2")){
            this.copy(legalAddress = legalAddress.copy(identifiers = listOf(sameIdentifier)))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(request1, request2))

        //THEN
        val expectedErrors = listOf(request1, request2).map { ErrorInfo(LegalEntityCreateError.LegalAddressDuplicateIdentifier, "IGNORED", it.index) }
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), expectedErrors)

        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal form
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with unknown legalform`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")){
            copy(legalEntity = legalEntity.copy(legalForm = "UNKNOWN"))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalFormNotFound, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown physical administrative region level 1
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with unknown physical region`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")){
            copy(legalAddress = legalAddress.copy(physicalPostalAddress = legalAddress.physicalPostalAddress.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressRegionNotFound, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown alternative administrative region level 1
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with unknown alternative region`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")){
            copy(legalAddress = legalAddress.copy(alternativePostalAddress = legalAddress.alternativePostalAddress!!.copy(administrativeAreaLevel1 = "UNKNOWN")))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressRegionNotFound, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown identifier type
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with unknown identifier type`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")){
            val unknownIdentifier = legalEntity.identifiers.first().copy(type = "UNKNOWN")
            copy(legalEntity = legalEntity.copy(identifiers = legalEntity.identifiers.drop(1).plus(unknownIdentifier)))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityIdentifierNotFound, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with unknown legal address identifier type
     * THEN operator sees error entry in response
     */
    @Test
    fun `try create legal entity with unknown legal address identifier type`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest("$testName 1")){
            val unknownIdentifier = legalAddress.identifiers.first().copy(type = "UNKNOWN")
            copy(legalAddress = legalAddress.copy(identifiers = legalAddress.identifiers.drop(1).plus(unknownIdentifier)))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressIdentifierNotFound, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with too many identifiers
     * THEN operator sees error entry in response
     *
     * ToDo:
     * At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try create legal entity with too many identifiers`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest(testName)){
            copy(legalEntity = legalEntity.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createLegalEntityIdentifier(testName, it) }))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalEntityIdentifiersTooMany, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    /**
     * WHEN operator tries to create a new legal entity with too many legal address identifiers
     * THEN operator sees error entry in response
     *
     * ToDo:
     * At the moment not as expected: https://github.com/eclipse-tractusx/bpdm/issues/1464
     */
    @Test
    @Disabled
    fun `try create legal entity with too many legal address identifiers`(){
        //WHEN
        val newLegalEntityRequest = with(testDataV6Factory.request.buildLegalEntityCreateRequest(testName)){
            copy(legalAddress = legalAddress.copy(identifiers = (1 .. 101).map { testDataV6Factory.request.createAddressIdentifier(testName, it) }))
        }

        val creationResponse = poolClient.legalEntities.createBusinessPartners(listOf(newLegalEntityRequest))

        //THEN
        val expectedError = ErrorInfo(LegalEntityCreateError.LegalAddressIdentifiersTooMany, "IGNORED", newLegalEntityRequest.index)
        val expectedResponse = LegalEntityPartnerCreateResponseWrapper(emptyList(), listOf(expectedError))
        assertRepo.assertLegalEntityCreate(creationResponse, expectedResponse)
    }

    private fun <T> createSingleResultPage(singleElement: T): PageDto<T> {
        return PageDto(1, 1, 0, 1, listOf(singleElement))
    }
}