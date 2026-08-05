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
        assertThat(createResult[0].businessPartner.legalEntity.ultimateOwnerBpnl).isEqualTo(createdBpnl)

        val createdFromPoolApi = poolClient.legalEntities.getLegalEntity(createdBpnl)
        assertThat(createdFromPoolApi.header.ownershipUltimate).isTrue()
        assertThat(createdFromPoolApi.header.ultimateOwnerBpnl).isEqualTo(createdBpnl)

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isTrue()
        assertThat(persistedEntity.ultimateOwnerBpnl).isEqualTo(createdBpnl)
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
        assertThat(updatedFromPoolApi.header.ultimateOwnerBpnl).isEqualTo(createdBpnl)

        val persistedEntity = legalEntityRepository.findByBpnIgnoreCase(createdBpnl)
        assertThat(persistedEntity).isNotNull()
        assertThat(persistedEntity!!.ownershipUltimate).isTrue()
        assertThat(persistedEntity.ultimateOwnerBpnl).isEqualTo(createdBpnl)
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
        val createdAddress = poolClient.addresses.getAddress(bpna).address
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
        assertThat(persistedEntity.ultimateOwnerBpnl).isEqualTo(createdBpnl)
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
        val ultimateOwner = ultimateOwnerResolutionService.resolveUltimateOwner(subsidiaryEntity)

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
        val ultimateOwner = ultimateOwnerResolutionService.resolveUltimateOwner(subsidiaryEntity)

        assertThat(ultimateOwner).isNull()
    }

    @Test
    fun `ultimate owner resolution - no ultimate owner when no relations present`() {
        val subsidiary = createLegalEntity("BPNL_S")

        val subsidiaryEntity = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val ultimateOwner = ultimateOwnerResolutionService.resolveUltimateOwner(subsidiaryEntity)

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
        val ultimateOwner = ultimateOwnerResolutionService.resolveUltimateOwner(subsidiaryEntity)
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
            result.relation.validityPeriods.size
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

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)

        val subsidiaryDbAfter = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val intermediateDbAfter = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ownershipUltimate).isTrue()
        assertThat(parentDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
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

        var subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        var intermediateDb = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!

        assertThat(subsidiaryDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(intermediateDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
        assertThat(parentDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)

        parentDb.ownershipUltimate = false
        parentDb.ultimateOwnerBpnl = null
        legalEntityRepository.save(parentDb)

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)

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

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(entityDb)

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

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)

        val subsidiaryDbAfter = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val intermediateDbAfter = legalEntityRepository.findByBpnIgnoreCase(intermediate.legalEntity.header.bpnl)!!

        assertThat(parentDb.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
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

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)

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

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)

        val child1DbAfter = legalEntityRepository.findByBpnIgnoreCase(child1.legalEntity.header.bpnl)!!
        val child2DbAfter = legalEntityRepository.findByBpnIgnoreCase(child2.legalEntity.header.bpnl)!!
        val grand1DbAfter = legalEntityRepository.findByBpnIgnoreCase(grand1.legalEntity.header.bpnl)!!
        val grand2DbAfter = legalEntityRepository.findByBpnIgnoreCase(grand2.legalEntity.header.bpnl)!!
        val parentDbAfter = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!

        assertThat(parentDbAfter.ultimateOwnerBpnl).isEqualTo(parent.legalEntity.header.bpnl)
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

        assertThat(exception.message).contains("Multiple ultimate owners detected")

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

    @Test
    fun `validation - reject second flag when ancestor is already flagged`() {
        val subsidiary = createLegalEntity("BPNL_VAL_S")
        val intermediate = createLegalEntity("BPNL_VAL_I")
        val parent = createLegalEntity("BPNL_VAL_P")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(parentDb)

        val subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        subsidiaryDb.ownershipUltimate = true
        legalEntityRepository.save(subsidiaryDb)

        val exception = org.junit.jupiter.api.assertThrows<BpdmValidationException> {
            ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(subsidiaryDb)
        }

        assertThat(exception.message).contains("Multiple ultimate owners detected")
        assertThat(exception.message).contains(parent.legalEntity.header.bpnl)
    }

    @Test
    fun `validation - reject second flag when descendant is already flagged`() {
        val subsidiary = createLegalEntity("BPNL_VAL_S2")
        val intermediate = createLegalEntity("BPNL_VAL_I2")
        val parent = createLegalEntity("BPNL_VAL_P2")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        subsidiaryDb.ownershipUltimate = true
        legalEntityRepository.save(subsidiaryDb)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(parentDb)

        val exception = org.junit.jupiter.api.assertThrows<BpdmValidationException> {
            ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(parentDb)
        }

        assertThat(exception.message).contains("Multiple ultimate owners detected")
        assertThat(exception.message).contains(subsidiary.legalEntity.header.bpnl)
    }

    @Test
    fun `validation - allow single flagged entity in hierarchy`() {
        val subsidiary = createLegalEntity("BPNL_VAL_S3")
        val intermediate = createLegalEntity("BPNL_VAL_I3")
        val parent = createLegalEntity("BPNL_VAL_P3")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(parentDb)

        ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(parentDb)

        val verifiedParent = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        assertThat(verifiedParent.ownershipUltimate).isTrue()
    }

    @Test
    fun `validation - allow moving flag from one entity to another`() {
        val subsidiary = createLegalEntity("BPNL_VAL_S4")
        val intermediate = createLegalEntity("BPNL_VAL_I4")
        val parent = createLegalEntity("BPNL_VAL_P4")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(parentDb)

        parentDb.ownershipUltimate = false
        legalEntityRepository.save(parentDb)

        val subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        subsidiaryDb.ownershipUltimate = true
        legalEntityRepository.save(subsidiaryDb)

        ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(subsidiaryDb)

        val verifiedSubsidiary = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val verifiedParent = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        assertThat(verifiedSubsidiary.ownershipUltimate).isTrue()
        assertThat(verifiedParent.ownershipUltimate).isFalse()
    }

    @Test
    fun `validation - cycle safety prevents infinite loop`() {
        val subsidiary = createLegalEntity("BPNL_VAL_CYCLE_S")
        val intermediate = createLegalEntity("BPNL_VAL_CYCLE_I")
        val parent = createLegalEntity("BPNL_VAL_CYCLE_P")

        createIsOwnedByRelation(subsidiary.legalEntity.header.bpnl, intermediate.legalEntity.header.bpnl)
        createIsOwnedByRelation(intermediate.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)
        createIsOwnedByRelation(parent.legalEntity.header.bpnl, subsidiary.legalEntity.header.bpnl)

        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(parentDb)

        val subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        subsidiaryDb.ownershipUltimate = true
        legalEntityRepository.save(subsidiaryDb)

        val exception = org.junit.jupiter.api.assertThrows<BpdmValidationException> {
            ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(subsidiaryDb)
        }

        assertThat(exception.message).contains("Multiple ultimate owners detected")
    }

    @Test
    fun `validation - error message includes conflicting flagged entities`() {
        val entity1 = createLegalEntity("BPNL_VAL_ERR1")
        val entity2 = createLegalEntity("BPNL_VAL_ERR2")
        val entity3 = createLegalEntity("BPNL_VAL_ERR3")

        createIsOwnedByRelationViaService(entity1.legalEntity.header.bpnl, entity2.legalEntity.header.bpnl)
        createIsOwnedByRelationViaService(entity2.legalEntity.header.bpnl, entity3.legalEntity.header.bpnl)

        val entity3Db = legalEntityRepository.findByBpnIgnoreCase(entity3.legalEntity.header.bpnl)!!
        entity3Db.ownershipUltimate = true
        legalEntityRepository.save(entity3Db)

        val entity1Db = legalEntityRepository.findByBpnIgnoreCase(entity1.legalEntity.header.bpnl)!!
        entity1Db.ownershipUltimate = true
        legalEntityRepository.save(entity1Db)

        val exception = org.junit.jupiter.api.assertThrows<BpdmValidationException> {
            ultimateOwnerResolutionService.validateOnlyOneUltimateOwnerInHierarchy(entity1Db)
        }

        assertThat(exception.message).contains("Multiple ultimate owners detected")
        assertThat(exception.message).contains(entity3.legalEntity.header.bpnl)
        assertThat(exception.message).contains("ownershipUltimate")
    }

    @Test
    fun `validation - no validation when flag changes from true to false`() {
        val subsidiary = createLegalEntity("BPNL_VAL_NOV_S")
        val parent = createLegalEntity("BPNL_VAL_NOV_P")

        createIsOwnedByRelationViaService(subsidiary.legalEntity.header.bpnl, parent.legalEntity.header.bpnl)

        val subsidiaryDb = legalEntityRepository.findByBpnIgnoreCase(subsidiary.legalEntity.header.bpnl)!!
        val parentDb = legalEntityRepository.findByBpnIgnoreCase(parent.legalEntity.header.bpnl)!!
        
        subsidiaryDb.ownershipUltimate = true
        parentDb.ownershipUltimate = true
        legalEntityRepository.save(subsidiaryDb)
        legalEntityRepository.save(parentDb)

        parentDb.ownershipUltimate = false
        legalEntityRepository.save(parentDb)

        ultimateOwnerResolutionService.updateUltimateOwnerForEntityAndDescendants(parentDb)
    }
}