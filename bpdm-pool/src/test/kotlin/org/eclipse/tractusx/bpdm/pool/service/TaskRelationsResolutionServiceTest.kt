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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
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
import java.time.Instant
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
            businessPartnerTargetBpnl = BusinessPartnerVerboseValues.secondBpnL,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
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
            businessPartnerTargetBpnl = savedEntity1.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
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
            businessPartnerTargetBpnl = savedEntity1.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
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
        assertThat(responseLegalEntity.relations.first().states.size).isEqualTo(createRelationsRequest.states.size)
    }


    @Test
    fun `IsManagedBy relation - handle all validity and merge scenarios`() {
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2.copy(
            legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2.legalEntity.copy(
                isParticipantData = true
            )
        )
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3.copy(
            legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate3.legalEntity.copy(
                isParticipantData = true
            )
        )

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        /**
         * Scenario 1: A→B exists, new A→C with non-overlapping validity → new relation created
         */
        val relationAB = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(RelationStateDto(validFrom = Instant.parse("2020-01-01T00:00:00Z"), validTo = Instant.parse("2020-12-31T23:59:59Z"), type = BusinessStateType.ACTIVE))
        )
        val resultAB = upsertRelationsGoldenRecordIntoPool("TASK_AB", relationAB)
        assertThat(resultAB[0].errors).isEmpty()

        val relationAC_nonOverlap = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(RelationStateDto(validFrom = Instant.parse("2021-01-01T00:00:00Z"), validTo = Instant.parse("2021-12-31T23:59:59Z"), type = BusinessStateType.ACTIVE))
        )
        val resultAC_nonOverlap = upsertRelationsGoldenRecordIntoPool("TASK_AC_NON_OVERLAP", relationAC_nonOverlap)
        assertThat(resultAC_nonOverlap[0].errors).isEmpty()

        /**
         * Scenario 2: A→C exists, new A→C with overlapping validity → extend existing validity
         */
        val relationAC_overlap = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(RelationStateDto(validFrom = Instant.parse("2021-06-01T00:00:00Z"), validTo = Instant.parse("2022-06-30T23:59:59Z"), type = BusinessStateType.ACTIVE))
        )
        val resultAC_overlap = upsertRelationsGoldenRecordIntoPool("TASK_AC_OVERLAP", relationAC_overlap)
        assertThat(resultAC_overlap[0].errors).isEmpty()

        // Fetch A→C and verify merged validity (should start 2021-01-01 and end 2022-06-30)
        // Verify merged validity on A→C
        val mergedRelationAC = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.bpnl))
            .legalEntity.relations.first { it.businessPartnerTargetBpnl == savedC.legalEntity.bpnl }
        val mergedState = mergedRelationAC.states.first()
        assertThat(mergedState.validFrom).isEqualTo(Instant.parse("2021-01-01T00:00:00Z"))
        assertThat(mergedState.validTo).isEqualTo(Instant.parse("2022-06-30T23:59:59Z"))

        /**
         * Scenario 3: A→B exists, new A→C overlaps in validity → should throw exception
         */
        val relationAC_conflict = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(RelationStateDto(validFrom = Instant.parse("2020-06-01T00:00:00Z"), validTo = Instant.parse("2020-09-30T23:59:59Z"), type = BusinessStateType.ACTIVE))
        )
        val resultAC_conflict = upsertRelationsGoldenRecordIntoPool("TASK_AC_CONFLICT", relationAC_conflict)
        assertThat(resultAC_conflict[0].errors).hasSize(1)
        assertThat(resultAC_conflict[0].errors[0].description).contains("Overlapping manager period exists")

        /**
         * Scenario 4: Same target, non-overlap → merge states into same relation
         */
        val relationAB_nonOverlap2 = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(RelationStateDto(validFrom = Instant.parse("2021-01-01T00:00:00Z"), validTo = Instant.parse("2021-12-31T23:59:59Z"), type = BusinessStateType.ACTIVE))
        )
        val resultAB_nonOverlap2 = upsertRelationsGoldenRecordIntoPool("TASK_AB_NON_OVERLAP", relationAB_nonOverlap2)
        assertThat(resultAB_nonOverlap2[0].errors).isEmpty()

        // Verify A→B now has two states merged
        val mergedRelationAB = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.bpnl))
            .legalEntity.relations.first { it.businessPartnerTargetBpnl == savedB.legalEntity.bpnl }
        assertThat(mergedRelationAB.states).hasSize(2)
    }

    /**
     * Tests the validation logic for 'IsManagedBy' relation when a target entity is not dataspace participant:
     *
     * GIVEN:
     *  - Two legal entities A and B
     *  - B is not dataspace participant
     *
     * WHEN:
     *  - Trying to create relation A → B of type 'IsManagedBy'
     *
     * THEN:
     *  - The request must fail because only dataspace participants can manage other entities.
     */
    @Test
    fun `create IsManagedBy relation - violate dataspace participant role`(){
        // Step 1: Create three legal entities A and B
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]

        // Step 2: Try to make A is managed by B -> should fail because of only dataspace participants can manage other entities
        val violatingDataspaceParticipantRole = BusinessPartnerRelations(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )

        val resultDataspaceParticipantViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_MANAGER", violatingDataspaceParticipantRole)

        //Step4: Assert error
        assertThat(resultDataspaceParticipantViolation.size).isEqualTo(1)
        val violationResult = resultDataspaceParticipantViolation.first()
        assertThat(violationResult.errors.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is owned by legal entity B
     * WHEN trying to create relation 'legal entity A is owned by legal entity C'
     * THEN return multiple parents error
     */
    @Test
    fun `create IsOwnedBy relation - violate single parent`(){
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        // Step 2: Create valid IsOwnedBy relation: A is owned by B
        val validRelation = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID", validRelation)

        // Step 3: Try to make A is owned by C -> should fail because of only one parent allowed
        val violatingSingleParent = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )

        val resultSingleParentViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_MANAGER", violatingSingleParent)

        //Step4: Assert error
        assertThat(resultSingleParentViolation.size).isEqualTo(1)
        val violationResult = resultSingleParentViolation.first()
        assertThat(violationResult.errors.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is owned by legal entity B which is owned by legal entity C
     * WHEN trying to create relation 'legal entity C is owned by legal entity A'
     * THEN return found cycles error
     */
    @Test
    fun `create IsOwnedBy relation - violate no cycles`(){
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        // Step 2: Create valid IsOwnedBy relation: A is owned by B
        val validAOwnedByB = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_1", validAOwnedByB)

        val validBOwnedByC = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedB.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_2", validBOwnedByC)

        // Step 3: Try to make C is owned by A -> should fail because of no cycles allowed
        val violatingNoCycles = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedC.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedA.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("1970-01-01T00:00:00Z"),
                    validTo = Instant.parse("9999-12-31T23:59:59Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )

        val resultNoCyclesViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_MANAGER", violatingNoCycles)

        //Step4: Assert error
        assertThat(resultNoCyclesViolation.size).isEqualTo(1)
        val violationResult = resultNoCyclesViolation.first()
        assertThat(violationResult.errors.size).isEqualTo(1)
    }

    @Test
    fun `create IsOwnedBy relation - handle new target, overlapping merge, and non-overlapping append`() {
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        // ===== Scenario 1: New target → Create new relation =====
        // Step 1: Create A→B (2025–2026)
        val relationAB = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedB.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("2025-01-01T00:00:00Z"),
                    validTo = Instant.parse("2026-01-01T00:00:00Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )

        upsertRelationsGoldenRecordIntoPool(
            "TASK_1",
            relationAB
        )

        // Step 2: Create A→C (2026–2027)
        val relationAC = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("2026-01-01T00:00:00Z"),
                    validTo = Instant.parse("2027-01-01T00:00:00Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )
        upsertRelationsGoldenRecordIntoPool(
            "TASK_2",
            relationAC
        )

        // Verify both relations exist
        val relationsAfterScenario1 = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.bpnl))
            .legalEntity.relations
        assertThat(relationsAfterScenario1).hasSize(2)
        assertThat(relationsAfterScenario1.map { it.businessPartnerTargetBpnl })
            .containsExactlyInAnyOrder(savedB.legalEntity.bpnl, savedC.legalEntity.bpnl)

        // ===== Scenario 2: Overlap → Merge =====
        // Step 3: Existing A→C (2026–2027), New A→C (2027–2029) → Merge to (2026–2029)
        // Step 2: Create A→C (2026–2027)
        val relationAC_Overlap = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("2027-01-01T00:00:00Z"),
                    validTo = Instant.parse("2029-01-01T00:00:00Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )

        upsertRelationsGoldenRecordIntoPool(
            "TASK_3",
            relationAC_Overlap
        )

        val relationACAfterMerge = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.bpnl))
            .legalEntity.relations.first { it.businessPartnerTargetBpnl == savedC.legalEntity.bpnl }
        assertThat(relationACAfterMerge.states).hasSize(1)
        assertThat(relationACAfterMerge.states.first().validFrom)
            .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"))
        assertThat(relationACAfterMerge.states.first().validTo)
            .isEqualTo(Instant.parse("2029-01-01T00:00:00Z"))

        // ===== Scenario 3: Non-overlap → Append =====
        // Step 4: Existing A→C (2026–2029), New A→C (2031–2032) → Append
        val relationAC_NonOverlap = BusinessPartnerRelations(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpnl = savedA.legalEntity.bpnl,
            businessPartnerTargetBpnl = savedC.legalEntity.bpnl,
            states = listOf(
                RelationStateDto(
                    validFrom = Instant.parse("2031-01-01T00:00:00Z"),
                    validTo = Instant.parse("2032-01-01T00:00:00Z"),
                    type = BusinessStateType.ACTIVE
                )
            )
        )
        upsertRelationsGoldenRecordIntoPool(
            "TASK_4",
            relationAC_NonOverlap
        )

        val relationACAfterAppend = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.bpnl))
            .legalEntity.relations.first { it.businessPartnerTargetBpnl == savedC.legalEntity.bpnl }
        assertThat(relationACAfterAppend.states).hasSize(2)
        assertThat(relationACAfterAppend.states.map { it.validFrom })
            .containsExactlyInAnyOrder(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2031-01-01T00:00:00Z")
            )
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