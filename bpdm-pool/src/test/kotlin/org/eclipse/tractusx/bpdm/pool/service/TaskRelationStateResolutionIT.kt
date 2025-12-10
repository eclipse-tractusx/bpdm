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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestDataEnvironment
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

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

    private lateinit var testDataEnvironment: TestDataEnvironment
    private lateinit var testName: String

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        dbTestHelpers.truncateDbTables()
        testDataEnvironment = dataHelper.createTestDataEnvironment()
        testName = testInfo.displayName
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `create valid limited legal entity relation`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo =  LocalDate.ofYearDay(2026, 1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create valid limited Address relation`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo =  LocalDate.ofYearDay(2026, 1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `create valid unlimited legal entity relation`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = null
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create valid unlimited address relation`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = null
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `create valid legal entity relation with inactivity`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create valid address relation with inactivity`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `create valid legal entity relation with unsorted states`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate.copy(validityPeriods = relationToCreate.validityPeriods.sortedBy { it.validFrom }))
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create valid address relation with unsorted states`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2026, 1)
                ),
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertSuccess(createdRelation, relationToCreate.copy(validityPeriods = relationToCreate.validityPeriods.sortedBy { it.validFrom }))
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `try create legal entity relation without validity`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf())
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `try create Address relation without validity`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf())
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(LegalEntityRelationType::class)
    fun `try create legal entity relation with overlap in states`(relationType: LegalEntityRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val legalEntity2 = createLegalEntity("$testName 2")

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity2.legalEntity.bpnl,
            legalEntity1.legalEntity.bpnl,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
                )
            ))
        val createdRelation = createRelation(relationToCreate)

        //Then
        assertError(createdRelation)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `try create address relation with overlap in states`(relationType: AddressRelationType){
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        //When
        val relationToCreate = BusinessPartnerRelations(
            relationType.toTaskDto(),
            legalEntity1.legalAddress.bpna,
            additionalAddress1.address.bpna,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2025, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2026, 1),
                    validTo = LocalDate.ofYearDay(2027, 1)
                ),
                RelationValidityPeriod(
                    validFrom = LocalDate.ofYearDay(2027, 1),
                    validTo = LocalDate.ofYearDay(2028, 1)
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

    private fun createAdditionalAddress(seed: String, legalEntity: LegalEntityPartnerCreateVerboseDto): AddressPartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.bpnl)
        return poolClient.addresses.createAddresses(listOf(request)).entities.single()
    }

    private fun LegalEntityRelationType.toTaskDto(): RelationType {
        return when(this){
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> RelationType.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> RelationType.IsOwnedBy
        }
    }

    private fun AddressRelationType.toTaskDto(): RelationType {
        return when(this){
            AddressRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }
    }
}