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

package org.eclipse.tractusx.bpdm.pool.service

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
import org.eclipse.tractusx.orchestrator.api.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationsResolutionServiceTest @Autowired constructor(
    val taskRelationsResolutionService: TaskRelationsResolutionService,
    val poolClient: PoolApiClient,
    val dbTestHelpers: DbTestHelpers,
    val poolDataHelpers: PoolDataHelpers
) {

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()
    }

    /*
    * GIVEN: An empty BusinessPartnerRelations request
    * WHEN: Attempting to upsert the empty relations into the pool
    * THEN: The operation should fail, returning an error message
    * */
    @Test
    fun `create empty relations`() {

        val createRelationsRequest = BusinessPartnerRelations.empty

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
    }

    /*
    * GIVEN: A relation request with non-existing source and target legal entities
    * WHEN: Attempting to upsert the relations into the pool
    * THEN: The operation should fail, returning an error message
    * */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create relations with non existing source and target legal entity`(relationType: RelationType) {

        val createRelationsRequest = BusinessPartnerRelations(
            relationType = relationType,
            businessPartnerSourceBpnl = BusinessPartnerVerboseValues.firstBpnL,
            businessPartnerTargetBpnl = BusinessPartnerVerboseValues.secondBpnL
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
    }

    /*
    * GIVEN: A created legal entity
    * WHEN: Attempting to create a relation where source and target are the same entity
    * THEN: The operation should fail, returning an error indicating self-relation is not allowed
    * */
    @ParameterizedTest
    @EnumSource(RelationType::class)
    fun `create relations with same source and target legal entity`(relationType: RelationType) {

        // Step 1: Create legal entity
        val entity1 = BusinessPartnerNonVerboseValues.legalEntityCreate1

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entity1))

        assertThat(response.entities.size).isEqualTo(1)
        val savedEntity1 = response.entities.toList()[0]

        val createRelationsRequest = BusinessPartnerRelations(
            relationType = relationType,
            businessPartnerSourceBpnl = savedEntity1.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedEntity1.legalEntity.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).isEqualTo("A legal entity cannot have a relation to itself (BPNL: ${savedEntity1.legalEntity.bpnl}).")
    }

    /*
    * GIVEN: Two created legal entities
    * WHEN: A valid relation request is made between the two entities
    * THEN: The operation should succeed without errors, and the relation should be retrievable by querying legal entity
    * */
    @Test
    fun `create relations with provided source and target legal entity`() {

        // Step 1: Create two legal entities
        val entity1 = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entity2 = BusinessPartnerNonVerboseValues.legalEntityCreate2

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entity1, entity2))

        assertThat(response.entities.size).isEqualTo(2)
        val savedEntity1 = response.entities.toList()[0]
        val savedEntity2 = response.entities.toList()[1]

        // Step 2: Create a relation request
        val createRelationsRequest = BusinessPartnerRelations(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpnl = savedEntity2.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedEntity1.legalEntity.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(0)

        // Step 3: Retrieve legal entity with the relation exists
        val bpnToFind = changeCase(savedEntity1.legalEntity.bpnl)
        val responseLegalEntity = poolClient.legalEntities.getLegalEntity(bpnToFind).legalEntity
        assertThat(responseLegalEntity.relations).isNotNull
        assertThat(responseLegalEntity.relations.first().type.name).isEqualTo(createRelationsRequest.relationType.name)
        assertThat(responseLegalEntity.relations.first().businessPartnerSourceBpnl).isEqualTo(createRelationsRequest.businessPartnerSourceBpnl)
        assertThat(responseLegalEntity.relations.first().businessPartnerTargetBpnl).isEqualTo(createRelationsRequest.businessPartnerTargetBpnl)
        assertThat(responseLegalEntity.relations.first().isActive).isEqualTo(true)
    }


    /**
     * Tests the validation logic for 'IsManagedBy' relation:
     *
     * GIVEN:
     *  - Three legal entities A, B, and C
     *  - A valid relation A → B of type 'IsManagedBy'
     *
     * WHEN:
     *  - Trying to create A → C: should fail because A is already managed (validateSingleManager)
     *  - Trying to create B → C: should fail because it creates a cycle (validateNoChain)
     *
     * THEN:
     *  - Both attempts must return an error in the response
     *  - Error messages should reflect the violated constraint
     */
    @Test
    fun `validate IsManagedBy relation - reject chain and multiple manager violation`() {
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        assertThat(response.entities.size).isEqualTo(3)
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        // Step 2: Create valid IsManagedBy relation: A is managed by B
        val validRelation = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl
        )

        val validResult = upsertRelationsGoldenRecordIntoPool("TASK_VALID", validRelation)
        assertThat(validResult[0].errors).isEmpty()

        // Step 3a: Try to make A managed by C -> should fail validateSingleManager
        val violatingSingleManager = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl
        )

        val resultSingleManagerViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_MANAGER", violatingSingleManager)
        assertThat(resultSingleManagerViolation[0].errors).hasSize(1)
        assertThat(resultSingleManagerViolation[0].errors[0].description).contains("already managed by another Managing Legal Entity")

        // Step 3b: Try to make B managed by C -> should fail validateNoChain
        val violatingChain = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedB.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl
        )

        val resultChainViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_CHAIN", violatingChain)
        assertThat(resultChainViolation[0].errors).hasSize(1)
        assertThat(resultChainViolation[0].errors[0].description).contains("already a Managing Legal Entity.")
    }


    private fun upsertRelationsGoldenRecordIntoPool(taskId: String, businessPartnerRelations: BusinessPartnerRelations): List<TaskRelationsStepResultEntryDto> {

        val taskStep = singleTaskStep(taskId = taskId, businessPartnerRelations = businessPartnerRelations)
        return taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(taskStep)
    }

    private fun singleTaskStep(taskId: String, businessPartnerRelations: BusinessPartnerRelations): List<TaskRelationsStepReservationEntryDto> {

        return listOf(
            TaskRelationsStepReservationEntryDto(
                taskId = taskId,
                recordId = UUID.randomUUID().toString(),
                businessPartnerRelations = businessPartnerRelations
            )
        )
    }

    private fun changeCase(value: String): String {
        return if (value.uppercase() != value)
            value.uppercase()
        else if (value.lowercase() != value)
            value.lowercase()
        else
            throw IllegalArgumentException("Can't change case of string $value")
    }

}