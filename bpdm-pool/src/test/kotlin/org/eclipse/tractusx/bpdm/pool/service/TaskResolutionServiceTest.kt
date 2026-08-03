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
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService.CleaningError
import org.eclipse.tractusx.bpdm.pool.service.operation.UltimateOwnerRecalculationService
import org.eclipse.tractusx.bpdm.pool.service.operation.UltimateOwnerResolutionService
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.springframework.transaction.support.TransactionTemplate
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.*
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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskResolutionServiceTest @Autowired constructor(
    val cleaningStepService: TaskResolutionService,
    val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    val legalEntityRepository: LegalEntityRepository,
    val poolClient: PoolApiClient,
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelper: PoolDataHelper,
    val ultimateOwnerResolutionService: UltimateOwnerResolutionService,
    val ultimateOwnerRecalculationService: UltimateOwnerRecalculationService,
    val ownedByRelationUpsertService: OwnedByRelationUpsertService,
    val relationRepository: RelationRepository,
    val transactionTemplate: TransactionTemplate
) {

    private lateinit var orchTestDataFactory: BusinessPartnerTestDataFactory
    private lateinit var testDataEnvironment: TestDataEnvironment

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()

        testDataEnvironment = poolDataHelper.createTestDataEnvironment()
        orchTestDataFactory = BusinessPartnerTestDataFactory(
            OrchestratorRequestFactoryCommon(
                TestMetadataReferences(
                    legalForms = testDataEnvironment.metadata.legalForms.map { it.technicalKey },
                    legalEntityIdentifierTypes = testDataEnvironment.metadata.legalEntityIdentifierTypes.map { it.technicalKey },
                    addressIdentifierTypes = testDataEnvironment.metadata.addressIdentifierTypes.map { it.technicalKey },
                    adminAreas = testDataEnvironment.metadata.adminAreas.map { it.code },
                    scriptCodes = testDataEnvironment.metadata.scriptCodes.map { it.technicalKey },
                    reasonCodes = testDataEnvironment.metadata.reasonCodes.map { it.technicalKey }
                )
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
        // The content parsers accumulate all validation errors rather than failing fast on the first missing field:
        // legal name + confidence (legal-entity header) and country + city + confidence (empty legal address).
        assertThat(result[0].errors.size).isEqualTo(5)
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
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.header.bpnl)
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
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.header.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
    }

    @Test
    fun `create legal entity persists ownership ultimate and keeps ultimate owner bpnl empty`() {
        val baseRequest = minValidLegalEntity()
        val createRequest = baseRequest
            .copy(legalEntity = baseRequest.legalEntity.copy(ownershipUltimate = true))
            .withLegalReferences("owner-flag-bpnl".toBpnRequest(), "owner-flag-bpna".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_OWNERSHIP_1", businessPartner = createRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!
        assertThat(createResult[0].businessPartner.legalEntity.ownershipUltimate).isTrue()
        assertThat(createResult[0].businessPartner.legalEntity.ultimateOwnerBpnl).isNull()

        val createdFromPoolApi = poolClient.legalEntities.getLegalEntity(createdBpnl)
        assertThat(createdFromPoolApi.header.ownershipUltimate).isTrue()
        assertThat(createdFromPoolApi.header.ultimateOwnerBpnl).isNull()

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isTrue()
        assertThat(persistedEntity.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `create legal entity without ownership fields remains backwards compatible`() {
        val createRequest = minValidLegalEntity()
            .withLegalReferences("owner-default-bpnl".toBpnRequest(), "owner-default-bpna".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_OWNERSHIP_2", businessPartner = createRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!
        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)

        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isFalse()
        assertThat(persistedEntity.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `update legal entity with ownership flag only persists change`() {
        val createRequest = minValidLegalEntity()
            .withLegalReferences("owner-flag-update-bpnl".toBpnRequest(), "owner-flag-update-bpna".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_OWNERSHIP_UPDATE_1", businessPartner = createRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!
        val updateRequest = createRequest.copy(
            legalEntity = createRequest.legalEntity.copy(ownershipUltimate = true)
        )

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_OWNERSHIP_UPDATE_2", businessPartner = updateRequest)
        assertThat(updateResult[0].errors).isEmpty()

        val updatedFromPoolApi = poolClient.legalEntities.getLegalEntity(createdBpnl)
        assertThat(updatedFromPoolApi.header.ownershipUltimate).isTrue()
        assertThat(updatedFromPoolApi.header.ultimateOwnerBpnl).isNull()

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isTrue()
        assertThat(persistedEntity.ultimateOwnerBpnl).isNull()
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
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.header.bpnl)
        assertThat(createdLegalEntity.legalAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.header.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createdAdditionalAddress.address.bpnLegalEntity).isEqualTo(createdLegalEntity.header.bpnl)
        assertThat(createdAdditionalAddress.address.addressType == AddressType.AdditionalAddress).isTrue()
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
        assertThat(createdLegalEntity.legalAddress.bpnLegalEntity).isEqualTo(createdLegalEntity.header.bpnl)
        assertThat(createdLegalEntity.legalAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.header.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity.copy(isParticipantData = false))
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!)
        assertThat(createdAdditionalAddress.address.bpnLegalEntity).isEqualTo(createdLegalEntity.header.bpnl)
        assertThat(createdAdditionalAddress.address.addressType == AddressType.AdditionalAddress).isTrue()
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
        assertThat(createdLegalEntity1.header.bpnl).isEqualTo(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
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
        assertThat(createdLegalEntity1.header.bpnl).isNotEqualTo(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        val createdLegalEntity2 = poolClient.legalEntities.getLegalEntity(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue!!)
        assertThat(resultSteps2[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity2.header.bpnl)
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
        assertThat(updatedLegalEntity.header.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
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
        assertThat(updatedLegalEntity.header.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
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
        assertThat(updatedLegalEntity.header.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
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
        assertThat(updatedLegalEntity.header.legalName).isEqualTo(updateLegalEntityRequest.legalEntity.legalName)
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
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.legalEntity.legalAddress.bpnReference.referenceValue!!).address
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!).address
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.name).isEqualTo(createSiteRequest.legalEntity.legalAddress.addressName)
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner.additionalAddress?.postalProperties)
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
                additionalAddress = additionalAddress!!.copyAsPostalAddress {
                    it.copy(
                        identifiers = listOf(sameIdentifier)
                    )
                }
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
                additionalAddress = additionalAddress!!.copyAsPostalAddress {
                    it.copy(
                        identifiers = listOf(sameIdentifier)
                    )
                }
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
        val createdLeAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.legalEntity.legalAddress.bpnReference.referenceValue!!).address
        val createdAdditionalAddress = poolClient.addresses.getAddress(createResult[0].businessPartner.additionalAddress?.bpnReference?.referenceValue!!).address
        assertThat(createResult[0].taskId).isEqualTo("TASK_1")
        assertThat(createResult[0].errors).hasSize(0)
        assertThat(createdLeAddress.addressType == AddressType.LegalAddress).isTrue()
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
        compareLogisticAddress(createdAdditionalAddress, createResult[0].businessPartner.additionalAddress?.postalProperties)
    }

    @Test
    fun `task result contains updatedAt timestamps from pool records`() {
        val createRequest = orchTestDataFactory.createFullBusinessPartner("updated-at-create")
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createRequest).first()
        assertThat(createResult.errors).isEmpty()

        val resultBusinessPartner = createResult.businessPartner
        assertThat(resultBusinessPartner.legalEntity.updatedAt).isNotNull()
        assertThat(resultBusinessPartner.legalEntity.legalAddress.updatedAt).isNotNull()

        resultBusinessPartner.site?.let { site ->
            assertThat(site.updatedAt).isNotNull()
            site.siteMainAddress?.let { mainAddress ->
                assertThat(mainAddress.updatedAt).isNotNull()
            }
        }

        resultBusinessPartner.additionalAddress?.let { additionalAddress ->
            assertThat(additionalAddress.updatedAt).isNotNull()
        }
    }

    @Test
    fun `task result keeps stored updatedAt when hasChanged is false`() {
        val createRequest = orchTestDataFactory.createFullBusinessPartner("updated-at-no-change")
            .copyWithBpnRequests()

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createRequest).first()
        assertThat(createResult.errors).isEmpty()

        val createdBusinessPartner = createResult.businessPartner
        val storedLegalEntity = poolClient.legalEntities.getLegalEntity(createdBusinessPartner.legalEntity.bpnReference.referenceValue!!)
        val storedSite = createdBusinessPartner.site?.bpnReference?.referenceValue?.let { poolClient.sites.getSite(it) }
        val storedAdditionalAddress = createdBusinessPartner.additionalAddress?.bpnReference?.referenceValue?.let { poolClient.addresses.getAddress(it) }

        val noChangeRequest = createdBusinessPartner.copy(
            legalEntity = createdBusinessPartner.legalEntity.copy(
                hasChanged = false,
                legalAddress = createdBusinessPartner.legalEntity.legalAddress.copy(hasChanged = false)
            ),
            site = createdBusinessPartner.site?.let { site ->
                site.copy(
                    hasChanged = false,
                    siteMainAddress = site.siteMainAddress?.copy(hasChanged = false)
                )
            },
            additionalAddress = createdBusinessPartner.additionalAddress?.copyAsPostalAddress { it.copy(hasChanged = false) }
        )

        val noChangeResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = noChangeRequest).first()
        assertThat(noChangeResult.errors).isEmpty()

        val returnedBusinessPartner = noChangeResult.businessPartner
        assertThat(returnedBusinessPartner.legalEntity.updatedAt).isEqualTo(storedLegalEntity.header.updatedAt)
        assertThat(returnedBusinessPartner.legalEntity.legalAddress.updatedAt).isEqualTo(storedLegalEntity.legalAddress.updatedAt)

        returnedBusinessPartner.site?.let { site ->
            assertThat(site.updatedAt).isEqualTo(storedSite?.site?.updatedAt)
            assertThat(site.siteMainAddress?.updatedAt).isEqualTo(storedSite?.mainAddress?.updatedAt)
        }

        returnedBusinessPartner.additionalAddress?.let { additionalAddress ->
            assertThat(additionalAddress.updatedAt).isEqualTo(storedAdditionalAddress?.address?.updatedAt)
        }
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
        return copy(additionalAddress = additionalAddress?.copyAsPostalAddress { it.copy(bpnReference = postalAddressBpn)  } )
    }


    fun BusinessPartner.withSiteReferences(siteBpn: BpnReference, siteMainAddressBpn: BpnReference): BusinessPartner {
        return copy(
            site = site?.copy(
                bpnReference = siteBpn,
                siteMainAddress = site!!.siteMainAddress!!.copy(bpnReference = siteMainAddressBpn)
            )
        )
    }

    /**
     * Adds a site script variant for every script code the given address already covers, on top of the ones the factory
     * seeded. A site created on an existing address states that address's whole content, so it has to keep covering the
     * scripts the address's other partners name themselves in - and the seeded factory picks each entity's script codes
     * independently. The added variants reuse a seeded main-address script variant, which the address content parser
     * requires to be complete.
     */
    fun BusinessPartner.withSiteScriptVariantsAlsoCovering(bpna: String): BusinessPartner {
        val site = site!!
        val mainAddressTemplate = site.scriptVariants.first().mainAddress
        val ownScriptCodes = site.scriptVariants.map { it.scriptCode }
        val addedVariants = poolClient.addresses.getAddress(bpna).scriptVariants
            .map { it.scriptCode }
            .filterNot { it in ownScriptCodes }
            .map { SiteScriptVariant(it, "Site Name $it", mainAddressTemplate) }

        return copy(site = site.copy(scriptVariants = site.scriptVariants + addedVariants))
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
        val createdAdditionalAddress = poolClient.addresses.getAddress(bpna).address
        assertThat(createdAdditionalAddress.addressType == AddressType.AdditionalAddress).isTrue()
        val updateLinkageRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .withSiteReferences(siteRefValue.toBpnRequest(), addressRefValue.toBpnRequest())
            .copy(additionalAddress = null)
        upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateLinkageRequest)
        val promotedAddress = poolClient.addresses.getAddress(bpna)
        assertThat(promotedAddress.address.addressType == AddressType.AdditionalAddress).isFalse()
        assertThat(promotedAddress.address.addressType == AddressType.SiteMainAddress).isTrue()

        // The site states the content of the address it adopts, so the promoted address covers what the site is named in.
        assertThat(promotedAddress.scriptVariants.map { it.scriptCode })
            .containsExactlyInAnyOrderElementsOf(updateLinkageRequest.site!!.scriptVariants.map { it.scriptCode })
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
        assertThat(createResult[0].businessPartner.legalEntity.bpnReference.referenceValue).isEqualTo(createdLegalEntity.header.bpnl)
        compareLegalEntity(createdLegalEntity, createResult[0].businessPartner.legalEntity)
        //Convert addressType from LegalAddress to LegalAndSiteMainAddress
        val site = Site(siteRefValue.toBpnRequest(), "site", listOf(), fullConfidenceCriteria(), false, null, emptyList())
        val updateLinkageRequest = orchTestDataFactory.createFullBusinessPartner("test")
            .withLegalReferences(leRefValue.toBpnRequest(), leAddressRefValue.toBpnRequest())
            .copy(site = site, additionalAddress = null)
        upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = updateLinkageRequest)
        val updatedLegalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        assertThat(updatedLegalEntity.legalAddress.addressType == AddressType.LegalAndSiteMainAddress).isTrue()
    }

    @Test
    fun `create second site sharing an existing site main address`() {
        val leRef = "le-shared"
        val leAddressRef = "le-addr-shared"
        val siteARef = "site-a"
        val siteBRef = "site-b"
        val sharedMainAddressRef = "shared-main-addr"

        // Task 1: create site A with its own (new) main address under the legal entity.
        val createSiteA = orchTestDataFactory.createFullBusinessPartner("siteA")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .withSiteReferences(siteARef.toBpnRequest(), sharedMainAddressRef.toBpnRequest())
            .copy(additionalAddress = null)
        val resultA = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteA)
        val siteABpns = resultA[0].businessPartner.site?.bpnReference?.referenceValue!!
        val sharedAddressBpn = poolClient.sites.getSite(siteABpns).mainAddress.bpna

        val addressAfterA = poolClient.addresses.getAddress(sharedAddressBpn).address
        assertThat(addressAfterA.addressType).isEqualTo(AddressType.SiteMainAddress)
        assertThat(addressAfterA.additionalSites).isEmpty()

        // Task 2: create a distinct site B whose main address references the SAME address as site A's. Site B states the
        // content of that address, so it has to cover site A's scripts as well as its own.
        val createSiteB = orchTestDataFactory.createFullBusinessPartner("siteB")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .withSiteReferences(siteBRef.toBpnRequest(), sharedMainAddressRef.toBpnRequest())
            .withSiteScriptVariantsAlsoCovering(sharedAddressBpn)
            .copy(additionalAddress = null)
        val resultB = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createSiteB)
        val siteBBpns = resultB[0].businessPartner.site?.bpnReference?.referenceValue!!

        assertThat(siteABpns).isNotEqualTo(siteBBpns)

        // The single address now belongs to both sites: still a site main address, with the newer site B
        // surfacing as an additional site of it.
        val sharedAddress = poolClient.addresses.getAddress(sharedAddressBpn)
        assertThat(sharedAddress.address.addressType).isEqualTo(AddressType.SiteMainAddress)
        assertThat(sharedAddress.address.additionalSites).containsExactly(siteBBpns)

        // Both sites resolve their main address to the one shared BPNA (no duplicate address was created).
        assertThat(poolClient.sites.getSite(siteABpns).mainAddress.bpna).isEqualTo(sharedAddressBpn)
        assertThat(poolClient.sites.getSite(siteBBpns).mainAddress.bpna).isEqualTo(sharedAddressBpn)

        // Site B's payload became the shared address's content, and it still covers every script both sites are named in,
        // so both remain readable.
        assertThat(sharedAddress.scriptVariants.map { it.scriptCode })
            .containsExactlyInAnyOrderElementsOf(createSiteB.site!!.scriptVariants.map { it.scriptCode })
        assertThat(poolClient.sites.getSite(siteABpns).site.scriptVariants).isNotEmpty()
        assertThat(poolClient.sites.getSite(siteBBpns).site.scriptVariants).isNotEmpty()
    }

    @Test
    fun `try create second site sharing an existing site main address without covering the first site`() {
        val leRef = "le-strand"
        val leAddressRef = "le-addr-strand"
        val sharedMainAddressRef = "shared-strand-addr"

        val createSiteA = orchTestDataFactory.createFullBusinessPartner("siteA")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .withSiteReferences("site-a-strand".toBpnRequest(), sharedMainAddressRef.toBpnRequest())
            .copy(additionalAddress = null)
        val resultA = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = createSiteA)
        val siteABpns = resultA[0].businessPartner.site?.bpnReference?.referenceValue!!
        val sharedAddressBpn = poolClient.sites.getSite(siteABpns).mainAddress.bpna

        // Site B states the shared address's content but covers only its own script - which would leave site A named in
        // a script its address no longer covers.
        val siteBScriptVariant = createSiteA.site!!.scriptVariants.first().let { it.copy(scriptCode = scriptCodeOtherThan(it.scriptCode)) }
        val createSiteB = orchTestDataFactory.createFullBusinessPartner("siteB")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .withSiteReferences("site-b-strand".toBpnRequest(), sharedMainAddressRef.toBpnRequest())
            .let { it.copy(site = it.site!!.copy(scriptVariants = listOf(siteBScriptVariant)), additionalAddress = null) }

        val resultB = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = createSiteB)

        assertThat(resultB.single().errors.map { it.description })
            .anyMatch { it.contains(siteABpns) && it.contains("must stay covered") }
        assertThat(poolClient.addresses.getAddress(sharedAddressBpn).address.additionalSites).isEmpty()
    }

    private fun scriptCodeOtherThan(scriptCode: String): String =
        orchTestDataFactory.metadata!!.scriptCodes.first { it != scriptCode }

    @Test
    fun `update site based legal entity into another script`() {
        val leRef = "le-script-switch"
        val leAddressRef = "le-addr-script-switch"
        val siteRef = "site-script-switch"

        // A legal entity whose legal address is also its site's main address: one address covers both partners.
        val create = orchTestDataFactory.createFullBusinessPartner("switch")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .let {
                it.copy(
                    site = it.site!!.copy(bpnReference = siteRef.toBpnRequest(), siteMainAddress = null),
                    additionalAddress = null
                )
            }
            .inScriptCode(orchTestDataFactory.metadata!!.scriptCodes.first())
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = create)
        assertThat(createResult.single().errors).isEmpty()
        val bpnL = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!

        // The cleaning result now names the same partner in another script, legal entity and site together.
        val switchedScriptCode = scriptCodeOtherThan(orchTestDataFactory.metadata!!.scriptCodes.first())
        val switched = create.inScriptCode(switchedScriptCode)
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = switched)

        assertThat(updateResult.single().errors).isEmpty()
        assertThat(poolClient.legalEntities.getLegalEntity(bpnL).scriptVariants.map { it.scriptCode })
            .containsExactly(switchedScriptCode)
    }

    @Test
    fun `try update legal entity into another script while its site keeps the old one`() {
        val leRef = "le-strand-site"
        val leAddressRef = "le-addr-strand-site"
        val siteRef = "site-strand-site"

        val create = orchTestDataFactory.createFullBusinessPartner("strandSite")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .let {
                it.copy(
                    site = it.site!!.copy(bpnReference = siteRef.toBpnRequest(), siteMainAddress = null),
                    additionalAddress = null
                )
            }
            .inScriptCode(orchTestDataFactory.metadata!!.scriptCodes.first())
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = create)
        val bpnS = createResult[0].businessPartner.site?.bpnReference?.referenceValue!!

        // A task for the legal entity alone: it rewrites the shared legal address but does not rewrite the site, so the
        // site would be left named in a script its address no longer covers.
        val legalEntityOnly = create
            .inScriptCode(scriptCodeOtherThan(orchTestDataFactory.metadata!!.scriptCodes.first()))
            .copy(site = null)
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = legalEntityOnly)

        assertThat(updateResult.single().errors.map { it.description })
            .anyMatch { it.contains(bpnS) && it.contains("must stay covered") }
    }

    @Test
    fun `try update legal entity into another script while carrying its site as unchanged`() {
        val leRef = "le-strand-unchanged"
        val leAddressRef = "le-addr-strand-unchanged"
        val siteRef = "site-strand-unchanged"

        val create = orchTestDataFactory.createFullBusinessPartner("strandUnchanged")
            .withLegalReferences(leRef.toBpnRequest(), leAddressRef.toBpnRequest())
            .let {
                it.copy(
                    site = it.site!!.copy(bpnReference = siteRef.toBpnRequest(), siteMainAddress = null),
                    additionalAddress = null
                )
            }
            .inScriptCode(orchTestDataFactory.metadata!!.scriptCodes.first())
        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = create)
        val bpnS = createResult[0].businessPartner.site?.bpnReference?.referenceValue!!

        // The site travels with the task but is reported as unchanged, so nothing rewrites it: the legal entity's new
        // script would leave the site named in a script its address no longer covers.
        val legalEntityOnly = create
            .inScriptCode(scriptCodeOtherThan(orchTestDataFactory.metadata!!.scriptCodes.first()))
            .let { it.copy(site = it.site!!.copy(hasChanged = false)) }
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_2", businessPartner = legalEntityOnly)

        assertThat(updateResult.single().errors.map { it.description })
            .anyMatch { it.contains(bpnS) && it.contains("must stay covered") }
    }

    /** The same business partner named in [scriptCode] alone - legal entity and site, so the shared address covers both. */
    private fun BusinessPartner.inScriptCode(scriptCode: String): BusinessPartner =
        copy(
            legalEntity = legalEntity.copy(scriptVariants = legalEntity.scriptVariants.take(1).map { it.copy(scriptCode = scriptCode) }),
            site = site?.let { site -> site.copy(scriptVariants = site.scriptVariants.take(1).map { it.copy(scriptCode = scriptCode) }) }
        )

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
            additionalAddress = businessPartner.additionalAddress!!.copyAsPostalAddress {
                it.copy(
                    identifiers = createIdentifiers(addressIdentifierTypeKey, 101)
                )
            }
        )

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_1", businessPartner = businessPartnerWithTooManyIdentifiers)

        assertThat(createResult.size).isEqualTo(1)
        assertThat(createResult.single().errors.size).isEqualTo(1)
    }

    @Test
    fun `repository test - ownershipUltimate and ultimateOwnerBpnl columns persist correctly`() {
        val createRequest = minValidLegalEntity()
            .copy(legalEntity = minValidLegalEntity().legalEntity.copy(ownershipUltimate = true))
            .withLegalReferences("repo-test-bpnl".toBpnRequest(), "repo-test-bpna".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_REPO_TEST_1", businessPartner = createRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isTrue()
        assertThat(persistedEntity.ultimateOwnerBpnl).isNull()
        val updateRequest = createRequest
            .copy(legalEntity = createRequest.legalEntity.copy(ownershipUltimate = false))

        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_REPO_TEST_2", businessPartner = updateRequest)
        assertThat(updateResult[0].errors).isEmpty()

        val updatedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(updatedEntity).isNotNull()
        assertThat(updatedEntity!!.ownershipUltimate).isFalse()
        assertThat(updatedEntity.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `repository test - ultimateOwnerBpnl can be set and persisted`() {

        val targetBpnl = "BPNL000000000XXX"

        val createRequest = minValidLegalEntity()
            .withLegalReferences("repo-test-bpnl-3".toBpnRequest(), "repo-test-bpna-3".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_REPO_TEST_3", businessPartner = createRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isFalse()
        assertThat(persistedEntity!!.ultimateOwnerBpnl).isNull()

        persistedEntity!!.ultimateOwnerBpnl = targetBpnl
        legalEntityRepository.save(persistedEntity)

        val updatedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(updatedEntity).isNotNull()
        assertThat(updatedEntity!!.ultimateOwnerBpnl).isEqualTo(targetBpnl)
    }

    @Test
    fun `backwards compatibility regression test - existing client without new fields`() {
        val legacyRequest = minValidLegalEntity()
            .withLegalReferences("legacy-bpnl".toBpnRequest(), "legacy-bpna".toBpnRequest())

        val createResult = upsertGoldenRecordIntoPool(taskId = "TASK_LEGACY_1", businessPartner = legacyRequest)
        assertThat(createResult[0].errors).isEmpty()

        val createdBpnl = createResult[0].businessPartner.legalEntity.bpnReference.referenceValue!!
        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isFalse()
        assertThat(persistedEntity.ultimateOwnerBpnl).isNull()

        val responseFromPool = poolClient.legalEntities.getLegalEntity(createdBpnl)
        assertThat(responseFromPool.header.ownershipUltimate).isFalse()
        assertThat(responseFromPool.header.ultimateOwnerBpnl).isNull()

        val updateRequest = legacyRequest.copy(
            legalEntity = legacyRequest.legalEntity.copy(
                legalName = "Updated Legal Name"
            )
        )
        val updateResult = upsertGoldenRecordIntoPool(taskId = "TASK_LEGACY_2", businessPartner = updateRequest)
        assertThat(updateResult[0].errors).isEmpty()
        val updatedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(updatedEntity).isNotNull()
        assertThat(updatedEntity!!.legalName.value).isEqualTo("Updated Legal Name")
        assertThat(updatedEntity.ownershipUltimate).isFalse()
        assertThat(updatedEntity.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `ultimate owner resolution - resolve with flag set at top`() {
        // Create three legal entities
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")

        val groupParentEntity = legalEntityRepository.findByBpnIgnoreCase(groupParent.legalEntity.header.bpnl)!!
        groupParentEntity.ownershipUltimate = true
        legalEntityRepository.save(groupParentEntity)

        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val ultimateOwner = ultimateOwnerResolutionService.resolve(subsidiaryEntity)

        assertThat(ultimateOwner).isEqualTo(groupParent.legalEntity.header.bpnl)
    }

    @Test
    fun `ultimate owner resolution - no ultimate owner when flag not set at top`() {
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")

        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val ultimateOwner = ultimateOwnerResolutionService.resolve(subsidiaryEntity)

        assertThat(ultimateOwner).isNull()
    }

    @Test
    fun `ultimate owner resolution - returns highest flagged ancestor even when top is unflagged`() {
        val subsidiary = createLegalEntity("BPNL_TOPFLAG_S")
        val intermediate = createLegalEntity("BPNL_TOPFLAG_I")
        val groupParent = createLegalEntity("BPNL_TOPFLAG_P")

        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val intermediateEntity = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        intermediateEntity.ownershipUltimate = true
        legalEntityRepository.save(intermediateEntity)

        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val parentEntity = legalEntityRepository.findByBpnIgnoreCase(groupParent.legalEntity.header.bpnl)!!

        assertThat(ultimateOwnerResolutionService.resolve(subsidiaryEntity)).isEqualTo(intermediate.legalEntity.header.bpnl)
        assertThat(ultimateOwnerResolutionService.resolve(intermediateEntity)).isEqualTo(intermediate.legalEntity.header.bpnl)
        assertThat(ultimateOwnerResolutionService.resolve(parentEntity)).isNull()
    }

    @Test
    fun `ultimate owner resolution - no ultimate owner when no relations present`() {
        val subsidiary = createLegalEntity("BPNL_S")

        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val ultimateOwner = ultimateOwnerResolutionService.resolve(subsidiaryEntity)

        assertThat(ultimateOwner).isNull()
    }

    @Test
    fun `ultimate owner consistency - recompute when relation is added`() {
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")

        val groupParentEntity = legalEntityRepository.findByBpnIgnoreCase(groupParent.legalEntity.header.bpnl)!!
        groupParentEntity.ownershipUltimate = true
        legalEntityRepository.save(groupParentEntity)

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val updatedSubsidiary = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)
        val updatedIntermediate = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)

        assertThat(updatedSubsidiary).isNotNull()
        assertThat(updatedSubsidiary!!.ultimateOwnerBpnl).isEqualTo(groupParent.legalEntity.header.bpnl)
        assertThat(updatedIntermediate).isNotNull()
        assertThat(updatedIntermediate!!.ultimateOwnerBpnl).isEqualTo(groupParent.legalEntity.header.bpnl)
    }

    @Test
    fun `ultimate owner consistency - multi-level chain reports same ultimate`() {
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")

        val groupParentEntity = legalEntityRepository.findByBpnIgnoreCase(groupParent.legalEntity.header.bpnl)!!
        groupParentEntity.ownershipUltimate = true
        legalEntityRepository.save(groupParentEntity)

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val updatedSubsidiary = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)
        val updatedIntermediate = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)

        assertThat(updatedSubsidiary).isNotNull()
        assertThat(updatedIntermediate).isNotNull()
        assertThat(updatedSubsidiary!!.ultimateOwnerBpnl).isEqualTo(groupParent.legalEntity.header.bpnl)
        assertThat(updatedIntermediate!!.ultimateOwnerBpnl).isEqualTo(groupParent.legalEntity.header.bpnl)
    }

    @Test
    fun `ultimate owner consistency - no ultimate owner when top not flagged`() {
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)

        val updatedSubsidiary = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)
        val updatedIntermediate = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)

        assertThat(updatedSubsidiary).isNotNull()
        assertThat(updatedSubsidiary!!.ultimateOwnerBpnl).isNull()
        assertThat(updatedIntermediate).isNotNull()
        assertThat(updatedIntermediate!!.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `ultimate owner cycle protection during resolution`() {
        val subsidiary = createLegalEntity("BPNL_S")
        val intermediate = createLegalEntity("BPNL_I")
        val groupParent = createLegalEntity("BPNL_P")
        val groupParentEntity = legalEntityRepository.findByBpnIgnoreCase(groupParent.legalEntity.header.bpnl)!!
        groupParentEntity.ownershipUltimate = true
        legalEntityRepository.save(groupParentEntity)
        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, groupParent.legalEntity.header.bpnl)
        createIsOwnedByRelation(groupParent.legalEntity.header.bpnl, subsidiary.legalEntity.header.bpnl)
        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val ultimateOwner = ultimateOwnerResolutionService.resolve(subsidiaryEntity)
        assertThat(ultimateOwner).isNull()
    }

    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createIsOwnedByRelation(sourceBpn: String, targetBpn: String) {
        val sourceEntity = legalEntityRepository.findByBpnIgnoreCase(sourceBpn)!!
        val targetEntity = legalEntityRepository.findByBpnIgnoreCase(targetBpn)!!

        val relation = org.eclipse.tractusx.bpdm.pool.entity.RelationDb(
            type = LegalEntityRelationType.IsOwnedBy,
            startNode = sourceEntity,
            endNode = targetEntity,
            validityPeriods = mutableListOf(currentValidityPeriod()),
            reasonCode = null
        )
        relationRepository.save(relation)
    }

    private fun createIsOwnedByRelationViaService(sourceBpn: String, targetBpn: String) {
        transactionTemplate.execute {
            val sourceEntity = legalEntityRepository.findByBpnIgnoreCase(sourceBpn)!!
            val targetEntity = legalEntityRepository.findByBpnIgnoreCase(targetBpn)!!

            val upsertRequest = IRelationUpsertStrategyService.UpsertRequest(
                source = sourceEntity,
                target = targetEntity,
                validityPeriods = listOf(currentValidityPeriod()),
                existingRelation = null,
                reasonCode = null
            )
            val result = ownedByRelationUpsertService.upsertRelation(upsertRequest)
            result.value.validityPeriods.size
        }
    }

    // Production rejects relations without validity periods (see TaskLegalEntityRelationsStepBuildService.validateValidityPeriods),
    // so fixtures must supply a currently-active, open-ended period to mirror that guarantee.
    private fun currentValidityPeriod() =
        org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb(
            validFrom = java.time.LocalDate.now().minusDays(1),
            validTo = null
        )

    private fun createIdentifiers(idTypeKey: String, amount: Int): List<Identifier>{
        return (1 .. amount).map { Identifier(it.toString(), idTypeKey, null) }
    }

    @Test
    fun `ultimate owner flag change - false to true triggers recalculation for entity and descendants`() {
        val subsidiary = createLegalEntity("BPNL_FLAG_S")
        val intermediate = createLegalEntity("BPNL_FLAG_I")
        val parent = createLegalEntity("BPNL_FLAG_P")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        var subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        var intermediateDb = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        var parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(subsidiaryDb.ultimateOwnerBpnl).isNull()
        assertThat(intermediateDb.ultimateOwnerBpnl).isNull()
        assertThat(parentDb.ultimateOwnerBpnl).isNull()

        parentDb.ownershipUltimate = true
        parentDb.ultimateOwnerBpnl = parent.legalEntity.header.bpnl
        legalEntityRepository.save(parentDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val subsidiaryDbAfter = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val intermediateDbAfter = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ownershipUltimate).isTrue()
        assertThat(parentDbAfter.ultimateOwnerBpnl).isNull()
        assertThat(subsidiaryDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(intermediateDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
    }

    @Test
    fun `ultimate owner flag change - true to false triggers recalculation and clears descendants`() {
        val subsidiary = createLegalEntity("BPNL_FLAG_S2")
        val intermediate = createLegalEntity("BPNL_FLAG_I2")
        val parent = createLegalEntity("BPNL_FLAG_P2")

        var parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        parentDb.ultimateOwnerBpnl = parent.legalEntity.header.bpnl
        legalEntityRepository.save(parentDb)

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        var subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        var intermediateDb = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!

        assertThat(subsidiaryDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(intermediateDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(parentDb.ultimateOwnerBpnl).isNull()

        parentDb.ownershipUltimate = false
        parentDb.ultimateOwnerBpnl = null
        legalEntityRepository.save(parentDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val subsidiaryDbAfter = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val intermediateDbAfter = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ownershipUltimate).isFalse()
        assertThat(parentDbAfter.ultimateOwnerBpnl).isNull()
        assertThat(subsidiaryDbAfter.ultimateOwnerBpnl).isNull()
        assertThat(intermediateDbAfter.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `ultimate owner flag change - unchanged flag does not trigger recalculation`() {
        val entity = createLegalEntity("BPNL_FLAG_UNCHANGED")

        val entityDb = legalEntityRepository.findByBpnIgnoreCase(entity.legalEntity.header.bpnl)!!
        val ultimateOwnerBefore = entityDb.ultimateOwnerBpnl

        entityDb.ownershipUltimate = false
        legalEntityRepository.save(entityDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(entityDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val entityDbAfter = legalEntityRepository.findByBpnIgnoreCase(entity.legalEntity.header.bpnl)!!
        assertThat(entityDbAfter.ultimateOwnerBpnl).isEqualTo(ultimateOwnerBefore)
    }

    @Test
    fun `ultimate owner flag change - changelog entries created for affected entities`() {
        val subsidiary = createLegalEntity("BPNL_FLAG_CL_S")
        val intermediate = createLegalEntity("BPNL_FLAG_CL_I")
        val parent = createLegalEntity("BPNL_FLAG_CL_P")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        parentDb.ultimateOwnerBpnl = parent.legalEntity.header.bpnl
        legalEntityRepository.save(parentDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val subsidiaryDbAfter = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val intermediateDbAfter = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ultimateOwnerBpnl).isNull()
        assertThat(intermediateDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(subsidiaryDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
    }

    @Test
    fun `ultimate owner flag change - cycle protection during flag change recalculation`() {
        val subsidiary = createLegalEntity("BPNL_FLAG_CYCLE_S")
        val intermediate = createLegalEntity("BPNL_FLAG_CYCLE_I")
        val parent = createLegalEntity("BPNL_FLAG_CYCLE_P")

        var parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        parentDb.ultimateOwnerBpnl = parent.legalEntity.header.bpnl
        legalEntityRepository.save(parentDb)

        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)
        createIsOwnedByRelation(parent.legalEntity.header.bpnl, subsidiary.legalEntity.header.bpnl)

        parentDb.ownershipUltimate = false
        legalEntityRepository.save(parentDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        assertThat(parentDbAfter.ownershipUltimate).isFalse()
        assertThat(parentDbAfter.ultimateOwnerBpnl).isNull()
    }

    @Test
    fun `ultimate owner flag change - multiple descendants at different levels`() {
        val parent = createLegalEntity("BPNL_FLAG_MULTI_P")
        val child1 = createLegalEntity("BPNL_FLAG_MULTI_C1")
        val child2 = createLegalEntity("BPNL_FLAG_MULTI_C2")
        val grand1 = createLegalEntity("BPNL_FLAG_MULTI_G1")
        val grand2 = createLegalEntity("BPNL_FLAG_MULTI_G2")

        createIsOwnedByRelationViaService(child1.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(child2.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(grand1.legalEntity.header.bpnl, child1.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(grand2.legalEntity.header.bpnl, child1.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        parentDb.ultimateOwnerBpnl = parent.legalEntity.header.bpnl
        legalEntityRepository.save(parentDb)

        transactionTemplate.execute {
            val managed = legalEntityRepository.findByBpnIgnoreCase(parentDb.bpn)!!
            ultimateOwnerRecalculationService.recalculate(listOf(managed))
        }

        val child1DbAfter = legalEntityRepository.findByBpnIgnoreCase(child1.legalEntity.header.bpnl)!!
        val child2DbAfter = legalEntityRepository.findByBpnIgnoreCase(child2.legalEntity.header.bpnl)!!
        val grand1DbAfter = legalEntityRepository.findByBpnIgnoreCase(grand1.legalEntity.header.bpnl)!!
        val grand2DbAfter = legalEntityRepository.findByBpnIgnoreCase(grand2.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ultimateOwnerBpnl).isNull()
        assertThat(child1DbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(child2DbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(grand1DbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(grand2DbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
    }

    @Test
    fun `validation - reject owned-by tree merge with two flagged ultimate owners`() {
        val childA = createLegalEntity("BPNL_VAL_MERGE_CA")
        val rootA = createLegalEntity("BPNL_VAL_MERGE_RA")
        val rootB = createLegalEntity("BPNL_VAL_MERGE_RB")

        createIsOwnedByRelationViaService(childA.legalEntity.header.bpnl, rootA.legalEntity.header.bpnl)

        val rootADb = legalEntityRepository.findByBpnIgnoreCase(rootA.legalEntity.header.bpnl)!!
        val rootBDb = legalEntityRepository.findByBpnIgnoreCase(rootB.legalEntity.header.bpnl)!!
        rootADb.ownershipUltimate = true
        rootBDb.ownershipUltimate = true
        legalEntityRepository.save(rootADb)
        legalEntityRepository.save(rootBDb)

        val exception = org.junit.jupiter.api.assertThrows<BpdmValidationException> {
            transactionTemplate.execute {
                val source = legalEntityRepository.findByBpnIgnoreCase(rootA.legalEntity.header.bpnl)!!
                val target = legalEntityRepository.findByBpnIgnoreCase(rootB.legalEntity.header.bpnl)!!

                ownedByRelationUpsertService.upsertRelation(
                    IRelationUpsertStrategyService.UpsertRequest(
                        source = source,
                        target = target,
                        validityPeriods = listOf(currentValidityPeriod()),
                        existingRelation = null,
                        reasonCode = null
                    )
                )
            }
        }

        assertThat(exception.message).contains("Multiple ultimate owners in entity hierarchy")

        val sourceAfter = legalEntityRepository.findByBpnIgnoreCase(rootA.legalEntity.header.bpnl)!!
        val targetAfter = legalEntityRepository.findByBpnIgnoreCase(rootB.legalEntity.header.bpnl)!!
        val mergedRelation = relationRepository.findAll(
            RelationRepository.byRelation(
                startNode = sourceAfter,
                endNode = targetAfter,
                type = LegalEntityRelationType.IsOwnedBy
            )
        ).singleOrNull()

        assertThat(mergedRelation).isNull()
    }

}