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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.service.application.GoldenRecordRelationTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class TaskRelationsRoutingIT @Autowired constructor(
    val relationTaskResolutionService: GoldenRecordRelationTaskResolutionApplicationService,
    val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    val poolClient: PoolV7Client,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers
) {

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        testName = testInfo.displayName
    }

    @Test
    fun `route legal entity IsOwnedBy relation to legal entity relation service`() {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val createLegalEntityRelationsRequest = BusinessPartnerRelations(
            relationType = LegalEntityRelationTypeV7.IsOwnedBy.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.header.bpnl,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            ),
            reasonCode = testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createLegalEntityRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).contains("BPNL")
        assertThat(result.errors.size).isEqualTo(0)
    }

    @Test
    fun `route legal entity IsAlternativeHeadquarterFor relation to legal entity relation service`() {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val createLegalEntityRelationsRequest = BusinessPartnerRelations(
            relationType = LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.header.bpnl,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            ),
            reasonCode = testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createLegalEntityRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).contains("BPNL")
        assertThat(result.errors.size).isEqualTo(0)
    }

    @Test
    fun `route legal entity IsManagedBy relation to legal entity relation service`() {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val createLegalEntityRelationsRequest = BusinessPartnerRelations(
            relationType = LegalEntityRelationTypeV7.IsManagedBy.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.header.bpnl,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    //Can't create relation in the past
                    validFrom = LocalDate.now().plusDays(1),
                    validTo = LocalDate.parse("9999-12-31")
                )
            ),
            reasonCode = testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createLegalEntityRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).contains("BPNL")
        assertThat(result.errors.size).isEqualTo(0)
    }

    @Test
    fun `route legal entity IsReplacedBy relation to legal entity relation service`() {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val createLegalEntityRelationsRequest = BusinessPartnerRelations(
            relationType = LegalEntityRelationTypeV7.IsReplacedBy.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.header.bpnl,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            ),
            reasonCode = testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createLegalEntityRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).contains("BPNL")
        assertThat(result.errors.size).isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `route address relation to address relation service`(relationType: AddressRelationTypeV7) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        val createAddressRelationsRequest = BusinessPartnerRelations(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = legalEntity1.legalEntity.legalAddress.bpna,
            businessPartnerTargetBpn = additionalAddress1.address.bpna,
            validityPeriods = listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.parse("1970-01-01"),
                    validTo = LocalDate.parse("9999-12-31")
                )
            ),
            reasonCode = testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )

        val result = upsertBusinessPartnerRelations(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result.taskId).isEqualTo("TASK_1")
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).isEqualTo(legalEntity1.legalEntity.legalAddress.bpna)
        assertThat(result.errors.size).isEqualTo(0)
    }

    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseV7Dto {
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createAdditionalAddress(seed: String, legalEntity: LegalEntityPartnerCreateVerboseV7Dto): AddressPartnerCreateVerboseV7Dto{
        val request = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.header.bpnl)
        return poolClient.addresses.createAddresses(listOf(request)).entities.single()
    }

    private fun upsertBusinessPartnerRelations(taskId: String, businessPartnerRelations: BusinessPartnerRelations) : TaskRelationsStepResultEntryDto {
        val taskEntry = singleTaskStep(taskId, businessPartnerRelations)
        orchestratorMockDataFactory.mockReservedRelationTasks(listOf(taskEntry))
        relationTaskResolutionService.resolveTasks()
        return orchestratorMockDataFactory.getRelationStepResults().single()
    }

    private fun singleTaskStep(taskId: String, businessPartnerRelations: BusinessPartnerRelations): TaskRelationsStepReservationEntryDto {

        return TaskRelationsStepReservationEntryDto(
                taskId = taskId,
                recordId = UUID.randomUUID().toString(),
                businessPartnerRelations = businessPartnerRelations
        )
    }

    private fun LegalEntityRelationTypeV7.toTaskDto(): RelationType {
        return when(this){
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> RelationType.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> RelationType.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> RelationType.IsReplacedBy
        }
    }

    private fun AddressRelationTypeV7.toTaskDto(): RelationType {
        return when(this){
            AddressRelationTypeV7.IsReplacedBy -> RelationType.IsReplacedBy
        }
    }

}