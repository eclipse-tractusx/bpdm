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

package org.eclipse.tractusx.bpdm.pool.service

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnRequests
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithLegalEntityIdentifiers
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithSiteMainAddress
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.Bpn
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType.BpnRequestIdentifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskResolutionServiceTest @Autowired constructor(
    val cleaningStepService: TaskResolutionService,
    val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    val poolClient: PoolApiClient,
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelper: PoolDataHelper
) {

    private lateinit var orchTestDataFactory: BusinessPartnerTestDataFactory
    private lateinit var testDataEnvironment: TestDataEnvironment

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()

        testDataEnvironment = poolDataHelper.createTestDataEnvironment()
        orchTestDataFactory = BusinessPartnerTestDataFactory(
            BusinessPartnerTestDataFactory.TestMetadataReferences(
                legalForms = testDataEnvironment.metadata.legalForms.map { it.technicalKey },
                legalEntityIdentifierTypes = testDataEnvironment.metadata.legalEntityIdentifierTypes.map { it.technicalKey },
                addressIdentifierTypes = testDataEnvironment.metadata.addressIdentifierTypes.map { it.technicalKey },
                adminAreas = testDataEnvironment.metadata.adminAreas.map { it.code }
            )
        )
    }

    @Test
    fun `create empty legal entity`() {

        val createLegalEntityRequest = BusinessPartner.empty.copy(
            legalEntity = LegalEntity.empty
        )

        val result = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
    }

    @Test
    fun `create legal entity without legal name`() {

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(legalEntity = legalEntity.copy(legalName = null))
        }

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertTaskError(createResult[0], "TASK_1", CleaningError.LEGAL_NAME_IS_NULL)
    }

    @Test
    fun `create min legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minValidLegalEntity()
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
    }

    @Test
    fun `create legal entity with all fields`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = null, additionalAddress = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(2)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
    }


    @Test
    fun `create legal entity with additional address`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val additionalAddressRefValue = "333"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withAdditionalAddressReference(additionalAddressRefValue.toBpnRequest())
            .copy(site = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, additionalAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(3)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdLegalEntity.legalAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
    }

    @Test
    fun `create legal entity with isCatenaXMemberData null`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val additionalAddressRefValue = "333"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withAdditionalAddressReference(additionalAddressRefValue.toBpnRequest())
            .withCxMembership(null)
            .copy(site = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val bpnMappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, additionalAddressRefValue))
        assertThat(bpnMappings.size).isEqualTo(3)

        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdLegalEntity.legalAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity.copy(isParticipantData = false))
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
    }

    @Test
    fun `create legal entity with invalid identifiers`() {

        val existingIdentifierType = testDataEnvironment.metadata.legalEntityIdentifierTypes.first().technicalKey

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    identifiers = listOf(
                        Identifier("same", existingIdentifierType, null),
                        Identifier("same", "Invalid", null)
                    )
                )
            )
        }.withLegalReferences("123".toBpnRequest(), "222".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create legal entity with invalid legal form`() {
        val createLegalEntityRequest = with(minValidLegalEntity()){ copy( legalEntity = legalEntity.copy(legalForm = "Invalid Form")) }
            .withLegalReferences("123".toBpnRequest(), "222".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create legal entity with invalid duplicate identifier`() {

        val identifierValue = "123"
        val existingIdentifierType = testDataEnvironment.metadata.legalEntityIdentifierTypes.first().technicalKey

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    identifiers = listOf(
                        Identifier(identifierValue, existingIdentifierType, null),
                        Identifier(identifierValue, existingIdentifierType, null)
                    )
                )
            )
        }.withLegalReferences("123".toBpnRequest(), "222".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create 2 legal entities with invalid duplicate identifier`() {

        val identifierValue = "123"
        val existingIdentifierType = testDataEnvironment.metadata.legalEntityIdentifierTypes.first().technicalKey

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    identifiers = listOf(
                        Identifier(identifierValue, existingIdentifierType, null)
                    )
                )
            )
        }.withLegalReferences("123".toBpnRequest(), "222".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val createLegalEntityRequest2 = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    identifiers = listOf(
                        Identifier(identifierValue, existingIdentifierType, null)
                    )
                )
            )
        }.withLegalReferences("987".toBpnRequest(), "654".toBpnRequest())

        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest2)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(1)
    }

    @Test
    fun `create legal entity with invalid address administrativeAreaLevel1`() {

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    legalAddress = legalEntity.legalAddress.copy(
                        physicalAddress = legalEntity.legalAddress.physicalAddress.copy(
                            administrativeAreaLevel1 = "Invalid"
                        )
                    )
                )
            )
        }.withLegalReferences("987".toBpnRequest(), "654".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create legal entity with invalid address identifier`() {

        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    legalAddress = legalEntity.legalAddress.copy(
                        identifiers = listOf(Identifier("value", "Invalid Ident", null) )
                    )
                )
            )
        }.withLegalReferences("987".toBpnRequest(), "654".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create legal entity with invalid duplicated address identifier`() {

        val availableIdentifierType = testDataEnvironment.metadata.addressIdentifierTypes.first().technicalKey
        val sameIdentifier =  Identifier("same value", availableIdentifierType, "Issuing Body 1")
        val createLegalEntityRequest = with(minValidLegalEntity()){
            copy(
                legalEntity = legalEntity.copy(
                    legalAddress =  legalEntity.legalAddress.copy(
                        identifiers = listOf(
                            sameIdentifier,
                            sameIdentifier
                        )
                    )
                )
            ).copyWithBpnRequests()
        }

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).isNotEmpty
    }


    @Test
    fun `check that requests with same referenceValue don't create a new legal entity`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minValidLegalEntity()
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)

        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest)
        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isEqualTo(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
    }

    @Test
    fun `create legal entity with different referenceValues `() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = minValidLegalEntity()
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)
        val createdLegalEntity1 = poolClient.legalEntities.getLegalEntity(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)

        val leRefValue2 = "diffenrentBpnL"
        val leAddressRefValue2 = "diffenrentBpnA"
        val createLegalEntityRequest2 = minValidLegalEntity()
            .withLegalReferences(leRefValue2.toBpnRequest(), leAddressRefValue2.toBpnRequest())

        val resultSteps2 = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createLegalEntityRequest2)
        val bpnMappings =
            bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(listOf(leRefValue, leAddressRefValue, leRefValue2, leAddressRefValue2))
        assertThat(bpnMappings.size).isEqualTo(4)

        assertThat(resultSteps2[0].taskId).isEqualTo("TASK_2")
        assertThat(resultSteps2[0].errors.size).isEqualTo(0)
        assertThat(createdLegalEntity1.legalEntity.bpnl).isNotEqualTo(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        val createdLegalEntity2 = poolClient.legalEntities.getLegalEntity(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity2.legalEntity.bpnl)
    }

    @Test
    fun `update legal entity with all fields by BPN`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("create")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = null, additionalAddress = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val createdLegalEntity = createResult.first().businessPartner.legalEntity
        val updateLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("update")
            .withLegalReferences(createdLegalEntity.bpnReference, createdLegalEntity.legalAddress.bpnReference)
            .copy(site = null, additionalAddress = null)

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner.legalEntity)
    }

    @Test
    fun `update Cx-Member legal entity without isCatenaXMemberData set`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("create")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withCxMembership(true)
            .copy(site = null, additionalAddress = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val createdLegalEntity = createResult.first().businessPartner.legalEntity
        val updateLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("update")
            .withLegalReferences(createdLegalEntity.bpnReference, createdLegalEntity.legalAddress.bpnReference)
            .withCxMembership(null)
            .copy(site = null, additionalAddress = null)

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner.legalEntity.copy(isParticipantData = createLegalEntityRequest.legalEntity.isParticipantData))
    }

    @Test
    fun `update Cx-Non-Member legal entity without isCatenaXMemberData set`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("create")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withCxMembership(false)
            .copy(site = null, additionalAddress = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val createdLegalEntity = createResult.first().businessPartner.legalEntity
        val updateLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("update")
            .withLegalReferences(createdLegalEntity.bpnReference, createdLegalEntity.legalAddress.bpnReference)
            .withCxMembership(null)
            .copy(site = null, additionalAddress = null)

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner.legalEntity.copy(isParticipantData = createLegalEntityRequest.legalEntity.isParticipantData))
    }

    @Test
    fun `update legal entity with all fields by BpnRequestIdentifier`() {

        val leRefValue = "123"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("create")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = null, additionalAddress = null)

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors.size).isEqualTo(0)

        // Update legal entity with same BpnRequestIdentifier
        val updateLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("update")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = null, additionalAddress = null)

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(0)

        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(updateResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(updatedLegalEntity.legalEntity.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
        compareLegalEntity(updatedLegalEntity, updateResult[0].businessPartner.legalEntity)
    }

    @Test
    fun `update legal entity invalid identifier type `() {

        val legalEntityRequest = "123"
        val legalAddressRequest = "234"
        val createLegalEntityRequest = orchTestDataFactory.createLegalEntityBusinessPartner("create")
            .withLegalReferences(legalEntityRequest.toBpnRequest(), legalAddressRequest.toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = orchTestDataFactory.createLegalEntityBusinessPartner("update")
            .withLegalReferences(legalEntityRequest.toBpnRequest(), legalAddressRequest.toBpnRequest())
            .copyWithLegalEntityIdentifiers(
                listOf(Identifier("value", "Invalid", null) )
            )

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)

        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `update legal entity invalid legal form `() {

        val legalEntityRequest = "123"
        val legalAddressRequest = "234"
        val createLegalEntityRequest = orchTestDataFactory.createLegalEntityBusinessPartner("create")
            .withLegalReferences(legalEntityRequest.toBpnRequest(), legalAddressRequest.toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = with(orchTestDataFactory.createLegalEntityBusinessPartner("update")){
            copy(legalEntity = legalEntity.copy(legalForm = "Invalid Form"))
        }.withLegalReferences(legalEntityRequest.toBpnRequest(), legalAddressRequest.toBpnRequest())

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `update legal entity not existing bpn `() {

        val createLegalEntityRequest = orchTestDataFactory.createLegalEntityBusinessPartner("create")
            .withLegalReferences("123".toBpnRequest(), "345".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        assertThat(createResult[0].errors.size).isEqualTo(0)

        val updateLegalEntityRequest = orchTestDataFactory.createLegalEntityBusinessPartner("update")
            .run {
                copy(
                    legalEntity = legalEntity.copy(bpnReference = BpnReference("InvalidBPN", null, Bpn))
                )
            }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateLegalEntityRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors.size).isEqualTo(1)
        assertThat(updateResult[0].errors[0].type).isEqualTo(TaskErrorType.Unspecified)
    }

    @Test
    fun `create Site with minimal fields`() {
        val createSiteRequest = minValidSite()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner.site?.bpnReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.siteName)
    }

    @Test
    fun `create Site with all fields`() {
        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create")
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdSite = poolClient.sites.getSite(createResult[0].businessPartner.site?.bpnReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdSite.site.name).isEqualTo(createSiteRequest.site?.siteName)
        compareSite(createdSite, createResult[0].businessPartner.site)
    }

    @Test
    fun `create site with additional address`() {
        val createSiteRequest = orchTestDataFactory.createFullBusinessPartner("create")
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.legalEntity.legalAddress.bpnReference.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.name).isEqualTo(createSiteRequest.legalEntity.legalAddress.addressName)
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner.additionalAddress)
        assertThat(createdAdditionalAddress.bpnLegalEntity).isEqualTo(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue)
        assertThat(createdAdditionalAddress.bpnSite).isEqualTo(createResult[0].businessPartner.site?.bpnReference?.referenceValue)
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
    }


    @Test
    fun `create Site without main address`() {

        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create")
            .copyWithSiteMainAddress(null)
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
    }

    @Test
    fun `create Site with invalid addresses administration level 1 and invalid identifier`() {

        val createSiteRequest = with(orchTestDataFactory.createSiteBusinessPartner("create") ){
            copy(
                site = site!!.copy(
                    siteMainAddress = site!!.siteMainAddress!!.copy(
                        identifiers = listOf(Identifier("value", "Invalid", null)),
                        physicalAddress = site!!.siteMainAddress!!.physicalAddress.copy(administrativeAreaLevel1 = "Invalid")
                    )
                )
            )
        }.copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).isNotEmpty
    }

    @Test
    fun `create Site with same identifier in main address and additional address`() {

        val sameIdentifier = Identifier("value", testDataEnvironment.metadata.legalEntityIdentifierTypes.first().technicalKey, null)
        val createSiteRequest = with(orchTestDataFactory.createFullBusinessPartner()){
            copy(
                site = site!!.copy(
                    siteMainAddress = site!!.siteMainAddress!!.copy(
                        identifiers = listOf(sameIdentifier)
                    )
                ),
                additionalAddress = additionalAddress!!.copy(
                    identifiers = listOf(sameIdentifier)
                )
            )
        }.copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(1)
    }

    @Test
    fun `update Site`() {
        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create").copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        val updateSiteRequest = with(createResult.first().businessPartner){
            orchTestDataFactory.createSiteBusinessPartner("update")
                .withLegalReferences(legalEntity.bpnReference, legalEntity.legalAddress.bpnReference)
                .withSiteReferences(site!!.bpnReference, site!!.siteMainAddress!!.bpnReference)
        }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateSiteRequest)
        val updatedSite = poolClient.sites.getSite(updateResult[0].businessPartner.site?.bpnReference?.referenceValue!!)
        compareSite(updatedSite, updateResult[0].businessPartner.site)
    }

    @Test
    fun `update Site with invalid bpnS`() {

        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create").copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        val updateSiteRequest = with(createResult.first().businessPartner){
            orchTestDataFactory.createSiteBusinessPartner("update")
                .withLegalReferences(legalEntity.bpnReference, legalEntity.legalAddress.bpnReference)
                .withSiteReferences(BpnReference("InvalidBPN", null, Bpn), site!!.siteMainAddress!!.bpnReference)
        }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateSiteRequest)
        assertThat(updateResult[0].errors).hasSize(1)
    }

    @Test
    fun `update Site with invalid address administration level 1 and invalid identifier`() {

        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create").copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        val updateSiteRequest = with(createResult.first().businessPartner){
            orchTestDataFactory.createSiteBusinessPartner("update")
                .withLegalReferences(legalEntity.bpnReference, legalEntity.legalAddress.bpnReference)
                .withSiteReferences(site!!.bpnReference, site!!.siteMainAddress!!.bpnReference)
        }.run{
            copy(
                site = site!!.copy(
                    siteMainAddress = site!!.siteMainAddress!!.copy(
                        identifiers = listOf(Identifier("value", "Invalid", null)),
                        physicalAddress = site!!.siteMainAddress!!.physicalAddress.copy(administrativeAreaLevel1 = "Invalid")
                    )
                )
            )
        }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateSiteRequest)
        assertThat(updateResult[0].taskId).isEqualTo("TASK_2")
        assertThat(updateResult[0].errors).isNotEmpty
    }

    @Test
    fun `update Site with same address identifiers in main address and additional address`() {
        val createSiteRequest = orchTestDataFactory.createFullBusinessPartner("create").copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        val sameIdentifier = Identifier("value", testDataEnvironment.metadata.legalEntityIdentifierTypes.first().technicalKey, null)
        val updateSiteRequest = with(createResult.first().businessPartner){
            orchTestDataFactory.createFullBusinessPartner("update")
                .withLegalReferences(legalEntity.bpnReference, legalEntity.legalAddress.bpnReference)
                .withSiteReferences(site!!.bpnReference, site!!.siteMainAddress!!.bpnReference)
        }.run{
            copy(
                site = site!!.copy(
                    siteMainAddress = site!!.siteMainAddress!!.copy(
                        identifiers = listOf(sameIdentifier)
                    )
                ),
                additionalAddress = additionalAddress!!.copy(
                    identifiers = listOf(sameIdentifier)
                )
            )
        }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateSiteRequest)
        assertThat(updateResult[0].errors).hasSize(1)
    }

    @Test
    fun `update Site with same reference value`() {
        val siteRef = "123"
        val createSiteRequest = orchTestDataFactory.createSiteBusinessPartner("create")
            .copyWithBpnRequests()
            .withSiteReferences(siteRef.toBpnRequest(), "345".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteRequest)

        val updateSiteRequest = with(createResult.first().businessPartner){
            orchTestDataFactory.createSiteBusinessPartner("update")
                .withLegalReferences(legalEntity.bpnReference, legalEntity.legalAddress.bpnReference)
                .withSiteReferences(siteRef.toBpnRequest(),"345".toBpnRequest())
        }

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = updateSiteRequest)
        assertThat(updateResult[0].errors).hasSize(0)
    }

    @Test
    fun `create address with all fields`() {

        val fullBpWithAddress = orchTestDataFactory.createFullBusinessPartner()
            .copy(site = null)
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = fullBpWithAddress)
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.legalEntity.legalAddress.bpnReference.referenceValue!!)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner.additionalAddress)
    }

    @Test
    fun `create multiple legal entity `() {

        val numberOfEntitiesToTest = 100
        val referenceIds = (1..numberOfEntitiesToTest).toList()
        val fullBpWithLegalEntity = referenceIds.map {
            orchTestDataFactory.createLegalEntityBusinessPartner("Legal Entity $it").copyWithBpnRequests()
        }

        val taskSteps = multipleTaskStep(fullBpWithLegalEntity)
        val createResults = cleaningStepService.upsertGoldenRecordIntoPool(taskSteps)
        assertThat(createResults).hasSize(numberOfEntitiesToTest)
        assertThat(createResults.filter { it.errors.isNotEmpty() }).hasSize(0)

        val updateResults = cleaningStepService.upsertGoldenRecordIntoPool(taskSteps)
        assertThat(updateResults).hasSize(numberOfEntitiesToTest)
        assertThat(updateResults.filter { it.errors.isNotEmpty() }).hasSize(0)
    }

    @Test
    fun `error on site update with wrong legal entity parent`() {
        val siteCreateRequest = orchTestDataFactory.createSiteBusinessPartner("SITE_ON_LE1").copyWithBpnRequests()
        upsertGoldenRecordIntoPool(
            taskId = "TASK_1",
            businessPartner = siteCreateRequest
        )

        val updateWithWrongLegalEntity = upsertGoldenRecordIntoPool(
            taskId = "TASK_2",
            businessPartner = orchTestDataFactory.createSiteBusinessPartner("SITE_ON_LE2").withSiteReferences(
                siteCreateRequest.site!!.bpnReference,
                siteCreateRequest.site!!.siteMainAddress!!.bpnReference
            )
        )

        assertThat(updateWithWrongLegalEntity[0].taskId).isEqualTo("TASK_2")
        assertThat(updateWithWrongLegalEntity[0].errors).hasSize(1)
        assertThat(updateWithWrongLegalEntity[0].errors[0].description).isEqualTo(CleaningError.SITE_WRONG_LEGAL_ENTITY_REFERENCE.message)
    }

    @Test
    fun `error on additional address update with wrong legal entity parent`() {
        val addressCreateRequest = orchTestDataFactory.createFullBusinessPartner("ADDRESS_ON_LE1").copyWithBpnRequests()
        upsertGoldenRecordIntoPool(
            taskId = "TASK_1",
            businessPartner = addressCreateRequest
        )

        val updateWithWrongLegalEntity = upsertGoldenRecordIntoPool(
            taskId = "TASK_2",
            businessPartner = orchTestDataFactory.createFullBusinessPartner("ADDRESS_ON_LE2").withAdditionalAddressReference(
                addressCreateRequest.additionalAddress!!.bpnReference
            )
        )

        assertThat(updateWithWrongLegalEntity[0].taskId).isEqualTo("TASK_2")
        assertThat(updateWithWrongLegalEntity[0].errors).hasSize(1)
        assertThat(updateWithWrongLegalEntity[0].errors[0].description).isEqualTo(CleaningError.ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE.message)
    }

    @Test
    fun `error on additional address update with wrong site parent`() {
        val addressCreateRequest = orchTestDataFactory.createFullBusinessPartner("ADDRESS_ON_SITE1").copyWithBpnRequests()
        upsertGoldenRecordIntoPool(
            taskId = "TASK_1",
            businessPartner = addressCreateRequest
        )

        val updateWithWrongSite = upsertGoldenRecordIntoPool(
            taskId = "TASK_2",
            businessPartner = orchTestDataFactory.createFullBusinessPartner("ADDRESS_ON_SITE2")
                .withLegalReferences(addressCreateRequest.legalEntity.bpnReference, addressCreateRequest.legalEntity.legalAddress.bpnReference)
                .withAdditionalAddressReference(addressCreateRequest.additionalAddress!!.bpnReference)
        )

        assertThat(updateWithWrongSite[0].taskId).isEqualTo("TASK_2")
        assertThat(updateWithWrongSite[0].errors).hasSize(1)
        assertThat(updateWithWrongSite[0].errors[0].description).isEqualTo(CleaningError.ADDITIONAL_ADDRESS_WRONG_SITE_REFERENCE.message)
    }


    fun upsertGoldenRecordIntoPool(taskId: String, businessPartner: BusinessPartner): List<TaskStepResultEntryDto> {

        val taskStep = singleTaskStep(taskId = taskId, businessPartner = businessPartner)
        return cleaningStepService.upsertGoldenRecordIntoPool(taskStep)
    }

    fun singleTaskStep(taskId: String, businessPartner: BusinessPartner): List<TaskStepReservationEntryDto> {

        return listOf(
            TaskStepReservationEntryDto(
                taskId = taskId,
                recordId = UUID.randomUUID().toString(),
                businessPartner = businessPartner
            )
        )
    }

    fun multipleTaskStep(businessPartners: List<BusinessPartner>): List<TaskStepReservationEntryDto> {

        return businessPartners.map {
            TaskStepReservationEntryDto(
                taskId = it.legalEntity.bpnReference.referenceValue!!,
                recordId = UUID.randomUUID().toString(),
                businessPartner = it
            )
        }

    }

    fun BusinessPartner.withAdditionalAddressReference(postalAddressBpn: BpnReference): BusinessPartner {
        return copy(additionalAddress = additionalAddress?.copy(bpnReference = postalAddressBpn))
    }


    fun BusinessPartner.withSiteReferences(siteBpn: BpnReference, siteMainAddressBpn: BpnReference): BusinessPartner {
        return copy(
            site = site?.copy(
                bpnReference = siteBpn,
                siteMainAddress = site!!.siteMainAddress!!.copy(bpnReference = siteMainAddressBpn)
                )
        )
    }

    fun BusinessPartner.withLegalReferences(legalEntityBpn: BpnReference, legalAddressBpn: BpnReference): BusinessPartner {
        return copy(
            legalEntity = legalEntity.copy(
                bpnReference = legalEntityBpn,
                legalAddress = legalEntity.legalAddress.copy(
                    bpnReference = legalAddressBpn
                )
            )
        )
    }

    fun BusinessPartner.withCxMembership(isCatenaXMemberData: Boolean?): BusinessPartner{
        return copy(legalEntity = legalEntity.copy(isParticipantData = isCatenaXMemberData))
    }

    private fun minValidLegalEntity(): BusinessPartner {
        return with(BusinessPartner.empty) {
        copy(
            legalEntity = legalEntity.copy(
                bpnReference = BpnReference(referenceValue = "BPNL REQUEST ID", null, referenceType = BpnRequestIdentifier),
                legalName = "Legal Name",
                confidenceCriteria = fullConfidenceCriteria(),
                legalAddress = minValidAddress().copy(confidenceCriteria = fullConfidenceCriteria())
            )
        )
        }
    }

    private fun minValidSite(): BusinessPartner {
        return with(BusinessPartner.empty) {
            copy(
                legalEntity = minValidLegalEntity().legalEntity,
                site =  Site.empty.copy(
                    bpnReference = BpnReference(referenceValue = "BPNS REQUEST ID", null, referenceType = BpnRequestIdentifier),
                    siteName = "Site Name",
                    confidenceCriteria = fullConfidenceCriteria(),
                    siteMainAddress = minValidAddress()
                )
            )
        }
    }

    private fun minValidAddress(): PostalAddress {
        return with(PostalAddress.empty){
            copy(
                confidenceCriteria = fullConfidenceCriteria(),
                physicalAddress = physicalAddress.copy(
                    country = CountryCode.DE.alpha2,
                    city = "Stuttgart"
                )
            )
        }
    }

    fun assertTaskError(step: TaskStepResultEntryDto, taskId: String, error: CleaningError) {

        assertThat(step.taskId).isEqualTo(taskId)
        assertThat(step.errors.size).isEqualTo(1)
        assertThat(step.errors[0].description).isEqualTo(error.message)
    }

    private fun String.toBpnRequest() = BpnReference(this, null, BpnRequestIdentifier)

    private fun fullConfidenceCriteria() =
        ConfidenceCriteria(
            sharedByOwner = true,
            numberOfSharingMembers = 1,
            checkedByExternalDataSource = true,
            lastConfidenceCheckAt = Instant.now(),
            nextConfidenceCheckAt = Instant.now().plus(1, ChronoUnit.DAYS),
            confidenceLevel = 10
        )

    @Test
    fun `update additional address to site main address`(){
        val leRefValue = "123"
        val siteRefValue = "1234"
        val leAddressRefValue = "222"
        val addressRefValue = "333"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withAdditionalAddressReference(addressRefValue.toBpnRequest())
            .copy(site = null)
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        val bpna = createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!
        val createdAdditionalAddress = poolClient.addresses.getAddress(bpna)
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
        val updateLinkageRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withSiteReferences(siteRefValue.toBpnRequest(), addressRefValue.toBpnRequest())
            .copy(additionalAddress = null)
        upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateLinkageRequest)
        val createdAddress = poolClient.addresses.getAddress(bpna)
        assertThat(createdAddress.addressType == AddressType.AdditionalAddress).isFalse()
        assertThat(createdAddress.addressType == AddressType.SiteMainAddress).isTrue()
    }

    @Test
    fun `update legal address to legal and site main address`(){
        val leRefValue = "123"
        val siteRefValue = "123_site"
        val leAddressRefValue = "222"
        val createLegalEntityRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = null, additionalAddress = null)
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createLegalEntityRequest)
        val bpnL = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!
        val createdLegalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isNotNull()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.legalEntity.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
        //Convert addressType from LegalAddress to LegalAndSiteMainAddress
        var site = Site(siteRefValue.toBpnRequest(), "site", listOf(), fullConfidenceCriteria(), false, null)
        val updateLinkageRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = site, additionalAddress = null)
        upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateLinkageRequest)
        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        assertThat(updatedLegalEntity.legalAddress.addressType == AddressType.LegalAndSiteMainAddress).isTrue()
    }

    @Test
    fun `create legal entity - too many identifiers`(){
        val legalIdentifierTypeKey = orchTestDataFactory.metadata!!.legalEntityIdentifierTypes.first()
        val addressIdentifierTypeKey = orchTestDataFactory.metadata!!.addressIdentifierTypes.first()

        val businessPartner = orchTestDataFactory.createLegalEntityBusinessPartner("test").copyWithBpnRequests()
        val businessPartnerWithTooManyIdentifiers = businessPartner.copy(
            legalEntity = businessPartner.legalEntity.copy(
                identifiers = createIdentifiers(legalIdentifierTypeKey, 101),
                legalAddress = businessPartner.legalEntity.legalAddress.copy(identifiers = createIdentifiers(addressIdentifierTypeKey, 101))
                )
        )

       val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = businessPartnerWithTooManyIdentifiers)

        assertThat(createResult.size).isEqualTo(1)
        assertThat(createResult.single().errors.size).isEqualTo(2)
    }

    @Test
    fun `create full new site - too many identifiers`(){
        val addressIdentifierTypeKey = orchTestDataFactory.metadata!!.addressIdentifierTypes.first()

        val businessPartner = orchTestDataFactory.createSiteBusinessPartner("test").copyWithBpnRequests()
        val businessPartnerWithTooManyIdentifiers = businessPartner.copy(
            site = businessPartner.site!!.copy(
                siteMainAddress = businessPartner.site!!.siteMainAddress!!.copy(identifiers = createIdentifiers(addressIdentifierTypeKey, 101))
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = businessPartnerWithTooManyIdentifiers)

        assertThat(createResult.size).isEqualTo(1)
        assertThat(createResult.single().errors.size).isEqualTo(1)
    }

    @Test
    fun `create full new additional address - too many identifiers`(){
        val addressIdentifierTypeKey = orchTestDataFactory.metadata!!.addressIdentifierTypes.first()

        val businessPartner = orchTestDataFactory.createFullBusinessPartner("test").copyWithBpnRequests()
        val businessPartnerWithTooManyIdentifiers = businessPartner.copy(
            additionalAddress = businessPartner.additionalAddress!!.copy(
                identifiers = createIdentifiers(addressIdentifierTypeKey, 101)
            )
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = businessPartnerWithTooManyIdentifiers)

        assertThat(createResult.size).isEqualTo(1)
        assertThat(createResult.single().errors.size).isEqualTo(1)
    }

    private fun createIdentifiers(idTypeKey: String, amount: Int): List<Identifier>{
        return (1 .. amount).map { Identifier(it.toString(), idTypeKey, null) }
    }
}
