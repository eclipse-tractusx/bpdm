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
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationStateResolutionIT @Autowired constructor(
    val taskRelationsResolutionService: TaskRelationsResolutionService,
    val poolClient: PoolApiClient,
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
    @EnumSource(RelationType::class)
    fun `create valid limited relation`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo =  futureDate.plusYears(1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create valid unlimited relation`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(1),
                    validTo = null
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create valid relation with inactivity`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                ),
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create valid relation with unsorted states`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(1),
                    validTo = futureDate.plusYears(2)
                ),
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                ),
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                ),
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate.copy(validityPeriods = relationToCreate.validityPeriods.sortedBy { it.validFrom }))
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `try create relation without validity`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf())
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }


    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `try create relation with overlap in states`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(2)
                ),
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(1),
                    validTo = futureDate.plusYears(2)
                ),
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(2),
                    validTo = futureDate.plusYears(3)
                )
            ))
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
            val relationToCreate = BusinessPartnerRelations(
                RelationType.IsManagedBy,
                legalEntity2.legalEntity.bpnl,
                legalEntity1.legalEntity.bpnl,
                listOf(
                    RelationValidityPeriod(
                        validFrom = pastDate.minusYears(1),
                        validTo = pastDate
                    )
                ))
            createRelation(relationToCreate)
        }


        //When
        val relationToUpdate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            )
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
            val relationToCreate = BusinessPartnerRelations(
                RelationType.IsManagedBy,
                legalEntity2.legalEntity.bpnl,
                legalEntity1.legalEntity.bpnl,
                listOf(
                    RelationValidityPeriod(
                        validFrom = pastDate.minusYears(1),
                        validTo = futureDate
                    )
                )
            )
            createRelation(relationToCreate)
        }

        //When
        val relationToUpdate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = pastDate.minusYears(1),
                    validTo = pastDate
                )
            )
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
            val relationToCreate = BusinessPartnerRelations(
                RelationType.IsManagedBy,
                legalEntity2.legalEntity.bpnl,
                legalEntity1.legalEntity.bpnl,
                listOf(
                    RelationValidityPeriod(
                        validFrom = pastDate,
                        validTo = futureDate
                    )
                ))
            createRelation(relationToCreate)
        }

        //When
        val relationToUpdate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = pastDate,
                    validTo = futureDate.plusYears(1)
                )
            )
        )
        val updatedRelation = createRelation(relationToUpdate)


        //Then
        assertSuccess(updatedRelation, relationToUpdate)
    }

    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `update relation with overwriting future validity`(relationType: RelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        val relationToCreate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            ))
        createRelation(relationToCreate)

        //When
        val relationToUpdate = BusinessPartnerRelations(
            relationType,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate.plusYears(2),
                    validTo = null
                )
            )
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

        val relationToCreate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = futureDate,
                    validTo = futureDate.plusYears(1)
                )
            ))
        createRelation(relationToCreate)

        //When
        val relationToUpdate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = pastDate.minusYears(1),
                    validTo = pastDate
                )
            )
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

        val relationToCreate = BusinessPartnerRelations(
            RelationType.IsManagedBy,
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = pastDate,
                    validTo = futureDate.plusYears(1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDto{
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createRelation(relation: BusinessPartnerRelations): TaskRelationsStepResultEntryDto{
        val entry =   TaskRelationsStepReservationEntryDto("any", "any", relation)
        return taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(listOf(entry)).single()
    }

    private fun assertSuccess(actual: TaskRelationsStepResultEntryDto, expectedContent: BusinessPartnerRelations){
        val expected = TaskRelationsStepResultEntryDto("any", expectedContent, emptyList())

        Assertions.assertThat(actual)
            .usingRecursiveAssertion()
            .ignoringFields(TaskRelationsStepResultEntryDto::taskId.name)
            .isEqualTo(expected)
    }

    private fun assertError(actual: TaskRelationsStepResultEntryDto){
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
}