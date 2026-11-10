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

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.pool.service.application.GoldenRecordRelationTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryV7Dto
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
import java.time.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth", "test-scheduling-disabled")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class TaskRelationStateResolutionIT @Autowired constructor(
    val relationTaskResolutionService: GoldenRecordRelationTaskResolutionApplicationService,
    val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    val poolClient: PoolV7Client,
    private val dataHelper: PoolDataHelper,
    private val dbTestHelpers: DbTestHelpers
) {
    private val pastClock = Clock.fixed(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String

    private val futureDate = LocalDate.now().plusYears(1)
    private val pastDate = LocalDate.now().minusYears(1)

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `create valid limited legal entity relation`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo =  futureDate.plusYears(1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `create valid limited Address relation`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo =  LocalDate.ofYearDay(2026, 1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `create valid unlimited legal entity relation`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(1),
                    validTo = null
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `create valid unlimited address relation`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = null
                )
            ),
            getAnyReasonCode()
            )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `create valid legal entity relation with inactivity`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `create valid address relation with inactivity`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `create valid legal entity relation with unsorted states`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(1),
                    validTo = futureDate.plusYears(2).minusDays(1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1).minusDays(1)
                ),
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate.copy(validityPeriods = relationToCreate.validityPeriods.sortedBy { it.validFrom }))
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `create valid address relation with unsorted states`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.of(2026, 12, 31)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.of(2025, 12, 31)
                ),
            ),
            getAnyReasonCode()
            )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate.copy(validityPeriods = relationToCreate.validityPeriods.sortedBy { it.validFrom }))
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `try create legal entity relation without validity`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `try create Address relation without validity`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `try create legal entity relation with overlap in states`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(2)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(1),
                    validTo = futureDate.plusYears(2)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @Test
    fun `try update IsManagedBy relation with overwriting historic validity`(){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        executeInPast {
            val relationToCreate = BusinessPartnerRelationsV7Dto(
                RelationTypeV7.IsManagedBy,
                legalEntity2.legalEntity.header.bpnl,
                legalEntity1.legalEntity.header.bpnl,
                listOf(
                    RelationValidityPeriodV7Dto(
                        validFrom = pastDate.minusYears(1),
                        validTo = pastDate
                    )
                ),
                reasonCode =  getAnyReasonCode()
            )
            createRelation(relationToCreate)
        }


        //When
        val relationToUpdate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val updatedRelation = createRelation(relationToUpdate)

        //Then
        assertError(updatedRelation)
    }

    @Test
    fun `try update relation with overwriting ongoing validity start`(){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        executeInPast {
            val relationToCreate = BusinessPartnerRelationsV7Dto(
                RelationTypeV7.IsManagedBy,
                legalEntity2.legalEntity.header.bpnl,
                legalEntity1.legalEntity.header.bpnl,
                listOf(
                    RelationValidityPeriodV7Dto(
                        validFrom = pastDate.minusYears(1),
                        validTo = futureDate
                    )
                ),
                reasonCode =  getAnyReasonCode()
            )
            createRelation(relationToCreate)
        }

        //When
        val relationToUpdate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = pastDate.minusYears(1),
                    validTo = pastDate
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val updatedRelation = createRelation(relationToUpdate)

        //Then
        assertError(updatedRelation)
    }

    @Test
    fun `update relation with overwriting ongoing validity end`(){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")


        executeInPast {
            val relationToCreate = BusinessPartnerRelationsV7Dto(
                RelationTypeV7.IsManagedBy,
                legalEntity2.legalEntity.header.bpnl,
                legalEntity1.legalEntity.header.bpnl,
                listOf(
                    RelationValidityPeriodV7Dto(
                        validFrom = pastDate,
                        validTo = futureDate
                    )
                ),
                reasonCode =  getAnyReasonCode()
            )
            createRelation(relationToCreate)
        }

        //When
        val relationToUpdate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = pastDate,
                    validTo = futureDate.plusYears(1)
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val updatedRelation = createRelation(relationToUpdate)


        //Then
        assertSuccess(updatedRelation, relationToUpdate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `update legal entity relation with overwriting future validity`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        createRelation(relationToCreate)

        //When
        val relationToUpdate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(2),
                    validTo = null
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val updatedRelation = createRelation(relationToUpdate)

        //Then
        assertSuccess(updatedRelation, relationToUpdate)
    }

    @Test
    fun `try update IsManagedBy relation with giving historic validity`(){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val relationToCreate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        createRelation(relationToCreate)

        //When
        val relationToUpdate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = pastDate.minusYears(1),
                    validTo = pastDate
                )
            ),
            reasonCode =  getAnyReasonCode()
        )
        val updatedRelation = createRelation(relationToUpdate)

        //Then
        assertError(updatedRelation)
    }

    @Test
    fun `try create IsManagedBy relation in the past`(){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val relationToCreate = BusinessPartnerRelationsV7Dto(
            RelationTypeV7.IsManagedBy,
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = pastDate,
                    validTo = futureDate.plusYears(1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `try create address relation with overlap in states`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationTypeV7::class)
    fun `try create legal entity relation with validity periods meeting on one date`(relationType: LegalEntityRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.header.bpnl,
            legalEntity1.legalEntity.header.bpnl,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = futureDate.plusYears(1),
                    validTo = futureDate.plusYears(2)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationTypeV7::class)
    fun `try create address relation with validity periods meeting on one date`(relationType: AddressRelationTypeV7){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelationsV7Dto(
            relationType.toTaskDto(),
            legalEntity1.legalEntity.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
                RelationValidityPeriodV7Dto(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                )
            ),
            getAnyReasonCode()
        )
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseV7Dto{
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createRelation(relation: BusinessPartnerRelationsV7Dto): TaskRelationsStepResultEntryV7Dto{
        val entry =   TaskRelationsStepReservationEntryV7Dto("any", "any", relation)
        return resolveRelationTasks(listOf(entry)).single()
    }

    private fun assertSuccess(actual: TaskRelationsStepResultEntryV7Dto, expectedContent: BusinessPartnerRelationsV7Dto){
        val expected = TaskRelationsStepResultEntryV7Dto("any", expectedContent, emptyList())

        Assertions.assertThat(actual)
            .usingRecursiveAssertion()
            .ignoringFields(TaskRelationsStepResultEntryV7Dto::taskId.name)
            .isEqualTo(expected)
    }

    private fun assertError(actual: TaskRelationsStepResultEntryV7Dto){
        Assertions.assertThat(actual.errors).isNotEmpty
    }

    private fun executeInPast(methodToExecute: () -> Unit){
        mockkStatic(Instant::class)
        mockkStatic(Clock::class)
        every { Instant.now() } returns pastClock.instant()
        every { Clock.systemUTC() } returns pastClock
        every { Clock.systemDefaultZone() } returns pastClock

        methodToExecute()
        unmockkStatic(Instant::class)
        unmockkStatic(Clock::class)
    }

    private fun createAdditionalAddress(seed: String, legalEntity: LegalEntityPartnerCreateVerboseV7Dto): AddressPartnerCreateVerboseV7Dto {
        val request = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.header.bpnl)
        return poolClient.addresses.createAddresses(listOf(request)).entities.single()
    }

    private fun LegalEntityRelationTypeV7.toTaskDto(): RelationTypeV7 {
        return when(this){
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> RelationTypeV7.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> RelationTypeV7.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> RelationTypeV7.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> RelationTypeV7.IsReplacedBy
        }
    }

    private fun AddressRelationTypeV7.toTaskDto(): RelationTypeV7 {
        return when(this){
            AddressRelationTypeV7.IsReplacedBy -> RelationTypeV7.IsReplacedBy
        }
    }

    private fun getAnyReasonCode(): String{
        return testDataEnvironment.metadata.reasonCodes.first().technicalKey
    }

    private fun resolveRelationTasks(tasks: List<TaskRelationsStepReservationEntryV7Dto>): List<TaskRelationsStepResultEntryV7Dto> {
        orchestratorMockDataFactory.mockReservedRelationTasks(tasks)
        relationTaskResolutionService.resolveTasks()
        return orchestratorMockDataFactory.getRelationStepResults()
    }
}
