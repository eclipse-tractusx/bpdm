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
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityPartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
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
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class]
)
@ActiveProfiles("test-no-auth")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class TaskRelationsResolutionServiceTest @Autowired constructor(
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
    @EnumSource(LegalEntityRelationType::class)
    fun `create relations with non existing source and target legal entity`(relationType: LegalEntityRelationType) {

        val createRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = BusinessPartnerVerboseValues.firstBpnL,
            businessPartnerTargetBpn = BusinessPartnerVerboseValues.secondBpnL
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
    @EnumSource(LegalEntityRelationType::class)
    fun `create relations with same source and target legal entity`(relationType: LegalEntityRelationType) {

        // Step 1: Create legal entity
        val entity1 = BusinessPartnerNonVerboseValues.legalEntityCreate1

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entity1))

        assertThat(response.entities.size).isEqualTo(1)
        val savedEntity1 = response.entities.toList()[0]

        val createRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = savedEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedEntity1.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).isEqualTo("A legal entity cannot have a relation to itself (BPNL: ${savedEntity1.legalEntity.header.bpnl}).")
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create relations with same source and target address`(relationType: AddressRelationType) {
        val legalEntity = createLegalEntity("$testName LE1")
        val address = createAdditionalAddress("$testName Addr1", legalEntity)

        val createRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = address.address.bpna,
            businessPartnerTargetBpn = address.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).isEqualTo("An Address cannot have a relation to itself (BPNA: ${createRelationsRequest.businessPartnerSourceBpn}).")
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
        val createRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = savedEntity2.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedEntity1.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(0)

        // Step 3: Retrieve legal entity with the relation exists
        val bpnToFind = changeCase(savedEntity1.legalEntity.header.bpnl)
        val responseLegalEntity = poolClient.legalEntities.getLegalEntity(bpnToFind).header
        assertThat(responseLegalEntity.relations).isNotNull
        assertThat(responseLegalEntity.relations.first().type.name).isEqualTo(createRelationsRequest.relationType.name)
        assertThat(responseLegalEntity.relations.first().businessPartnerSourceBpnl).isEqualTo(createRelationsRequest.businessPartnerSourceBpn)
        assertThat(responseLegalEntity.relations.first().businessPartnerTargetBpnl).isEqualTo(createRelationsRequest.businessPartnerTargetBpn)
        assertThat(responseLegalEntity.relations.first().validityPeriods.size).isEqualTo(createRelationsRequest.validityPeriods.size)
        assertThat(responseLegalEntity.relations.first().reasonCode).isEqualTo(createRelationsRequest.reasonCode)
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `create relations with provided source and target address`(relationType: AddressRelationType) {
        // Step 1: Create legal entity and additional address
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)
        // Step 2: Create a relation request
        val createAddressRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = legalEntity1.legalEntity.legalAddress.bpna,
            businessPartnerTargetBpn = additionalAddress1.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].businessPartnerRelations.businessPartnerSourceBpn).isEqualTo(legalEntity1.legalEntity.legalAddress.bpna)
        assertThat(result[0].errors.size).isEqualTo(0)

        // Step 3: Retrieve address with the relation exists
        val bpnToFind = changeCase(additionalAddress1.address.bpna)
        val responseAddressRelation = poolClient.addresses.getAddress(bpnToFind).address.relations
        assertThat(responseAddressRelation).isNotNull
        assertThat(responseAddressRelation.first().type.name).isEqualTo(createAddressRelationsRequest.relationType.name)
        assertThat(responseAddressRelation.first().businessPartnerSourceBpna).isEqualTo(createAddressRelationsRequest.businessPartnerSourceBpn)
        assertThat(responseAddressRelation.first().businessPartnerTargetBpna).isEqualTo(createAddressRelationsRequest.businessPartnerTargetBpn)
        assertThat(responseAddressRelation.first().validityPeriods.size).isEqualTo(createAddressRelationsRequest.validityPeriods.size)
        assertThat(responseAddressRelation.first().reasonCode).isEqualTo(createAddressRelationsRequest.reasonCode)
    }


    @Test
    fun `IsManagedBy relation - handle all validity scenarios`() {
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = with(BusinessPartnerNonVerboseValues.legalEntityCreate2){
            copy(
                legalEntity = legalEntity.copy(header = legalEntity.header.copy(isParticipantData = true))
            )
        }
        val entityC = with(BusinessPartnerNonVerboseValues.legalEntityCreate3){
            copy(
                legalEntity = legalEntity.copy(header = legalEntity.header.copy(isParticipantData = true))
            )
        }

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        val nowDate = LocalDate.now()

        /**
         * Scenario 1: A→B exists
         */
        val relationAB = buildRelationRequest(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl,
            validityPeriods = listOf(RelationValidityPeriod(
                validFrom = nowDate.plusYears(1),
                validTo = nowDate.plusYears(2)
            ))
        )
        val resultAB = upsertRelationsGoldenRecordIntoPool("TASK_AB", relationAB)
        assertThat(resultAB[0].errors).isEmpty()

        /**
         * Scenario 2: A→B exists, new A→C overlaps in validity → should throw exception
         */
        val relationAC_conflict = buildRelationRequest(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl,
            validityPeriods = listOf(RelationValidityPeriod(
                validFrom = nowDate.plusYears(1).plusMonths(6),
                validTo = nowDate.plusYears(1).plusMonths(9)
            ))
        )
        val resultAC_conflict = upsertRelationsGoldenRecordIntoPool("TASK_AC_CONFLICT", relationAC_conflict)
        assertThat(resultAC_conflict[0].errors).hasSize(1)

        /**
         * Scenario 3: A→C exists, new A→C with overlapping validity → overwrite existing validity
         */
        val relationAC_nonOverlap = buildRelationRequest(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl,
            validityPeriods = listOf(RelationValidityPeriod(
                validFrom = nowDate.plusYears(2),
                validTo = nowDate.plusYears(3)
            ))
        )
        val resultAC_nonOverlap = upsertRelationsGoldenRecordIntoPool("TASK_AC_NON_OVERLAP", relationAC_nonOverlap)
        assertThat(resultAC_nonOverlap[0].errors).isEmpty()

        val relationAC_new = buildRelationRequest(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl,
            validityPeriods = listOf(RelationValidityPeriod(
                validFrom = nowDate.plusYears(2),
                validTo = nowDate.plusYears(3).plusMonths(6)
            ))
        )
        val resultAC_new = upsertRelationsGoldenRecordIntoPool("TASK_AC_NEW", relationAC_new)
        assertThat(resultAC_new[0].errors).isEmpty()

        // Fetch A→C and verify it now has only the newly proposed state (overwrite, no merge)
        val updatedRelationAC = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.header.bpnl))
            .header.relations.first { it.businessPartnerTargetBpnl == savedC.legalEntity.header.bpnl }
        assertThat(updatedRelationAC.validityPeriods).hasSize(1)
        val stateAC = updatedRelationAC.validityPeriods.first()
        assertThat(stateAC.validFrom).isEqualTo(nowDate.plusYears(2))
        assertThat(stateAC.validTo).isEqualTo(nowDate.plusYears(3).plusMonths(6))
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
        val violatingDataspaceParticipantRole = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsManagedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
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
        val validRelation = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID", validRelation)

        // Step 3: Try to make A is owned by C -> should fail because of only one parent allowed
        val violatingSingleParent = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
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
        val validAOwnedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_1", validAOwnedByB)

        val validBOwnedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_2", validBOwnedByC)

        // Step 3: Try to make C is owned by A -> should fail because of no cycles allowed
        val violatingNoCycles = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedC.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedA.legalEntity.header.bpnl
        )

        val resultNoCyclesViolation = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_MANAGER", violatingNoCycles)

        //Step4: Assert error
        assertThat(resultNoCyclesViolation.size).isEqualTo(1)
        val violationResult = resultNoCyclesViolation.first()
        assertThat(violationResult.errors.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is owned by legal entity B
     * AND legal entity B is owned by legal entity C
     * WHEN trying to create relation 'B is owned by C' (chain extension)
     * AND update existing relation 'A is owned by B' with new validity period
     * THEN both operations should succeed without errors
     */
    @Test
    fun `create IsOwnedBy relation - chain extension and update existing`() {
        // Step 1: Create three legal entities A, B, C
        val entityA = BusinessPartnerNonVerboseValues.legalEntityCreate1
        val entityB = BusinessPartnerNonVerboseValues.legalEntityCreate2
        val entityC = BusinessPartnerNonVerboseValues.legalEntityCreate3

        val response = poolClient.legalEntities.createBusinessPartners(listOf(entityA, entityB, entityC))
        val savedA = response.entities.toList()[0]
        val savedB = response.entities.toList()[1]
        val savedC = response.entities.toList()[2]

        // Step 2: Create initial IsOwnedBy relation: A is owned by B
        val relationAOwnedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        val resultInitial = upsertRelationsGoldenRecordIntoPool("TASK_INITIAL", relationAOwnedByB)
        assertThat(resultInitial[0].errors).isEmpty()

        // Step 3: Chain Extension - B is owned by C
        val relationBOwnedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )
        val resultChainExtension = upsertRelationsGoldenRecordIntoPool("TASK_CHAIN_EXTENSION", relationBOwnedByC)
        assertThat(resultChainExtension[0].errors).isEmpty()

        // Step 4: Update existing A → B with new validity period (non-overlapping)
        val updatedRelationAOwnedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        val resultUpdate = upsertRelationsGoldenRecordIntoPool("TASK_UPDATE_EXISTING", updatedRelationAOwnedByB)
        assertThat(resultUpdate[0].errors).isEmpty()

        // Step 5: Verify via API that both relations exist with expected states
        val fetchedA = poolClient.legalEntities.getLegalEntity(changeCase(savedA.legalEntity.header.bpnl))
        val fetchedB = poolClient.legalEntities.getLegalEntity(changeCase(savedB.legalEntity.header.bpnl))

        val relationAtoB = fetchedA.header.relations
            .firstOrNull { it.businessPartnerTargetBpnl == savedB.legalEntity.header.bpnl }
        assertThat(relationAtoB).isNotNull
        assertThat(relationAtoB!!.validityPeriods.size).isEqualTo(1)

        val relationBtoC = fetchedB.header.relations
            .firstOrNull { it.businessPartnerTargetBpnl == savedC.legalEntity.header.bpnl }
        assertThat(relationBtoC).isNotNull
        assertThat(relationBtoC!!.validityPeriods.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is replaced by legal entity B
     * WHEN trying to create relation 'legal entity A is replaced by legal entity C' in an overlapping validity period
     * THEN return multiple successors error
     */
    @Test
    fun `create IsReplacedBy relation - violate single successor`() {
        val savedA = createLegalEntity("$testName A")
        val savedB = createLegalEntity("$testName B")
        val savedC = createLegalEntity("$testName C")

        val validReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID", validReplacedByB)

        val violatingSingleSuccessor = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_SINGLE_SUCCESSOR", violatingSingleSuccessor).single()

        assertThat(result.errors.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is replaced by legal entity C
     * WHEN creating relation 'legal entity B is replaced by legal entity C' in an overlapping validity period
     * THEN the merger of both predecessors into one successor is accepted
     */
    @Test
    fun `create IsReplacedBy relation - allow merger into a shared successor`() {
        val savedA = createLegalEntity("$testName A")
        val savedB = createLegalEntity("$testName B")
        val savedC = createLegalEntity("$testName C")

        val aReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_FIRST_PREDECESSOR", aReplacedByC)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool("TASK_SECOND_PREDECESSOR", bReplacedByC).single()

        assertThat(result.errors).isEmpty()
    }

    /**
     * GIVEN legal entity A is replaced by legal entity B which is replaced by legal entity C
     * WHEN trying to create relation 'legal entity C is replaced by legal entity A'
     * THEN return found cycles error
     */
    @Test
    fun `create IsReplacedBy relation - violate no cycles`() {
        val savedA = createLegalEntity("$testName A")
        val savedB = createLegalEntity("$testName B")
        val savedC = createLegalEntity("$testName C")

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_1", aReplacedByB)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_2", bReplacedByC)

        val violatingNoCycles = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedC.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedA.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_NO_CYCLES", violatingNoCycles).single()

        assertThat(result.errors.size).isEqualTo(1)
    }

    /**
     * GIVEN legal entity A is replaced by legal entity B
     * WHEN creating relation 'legal entity B is replaced by legal entity C'
     * THEN the succession chain is accepted and both relations are reported on B
     */
    @Test
    fun `create IsReplacedBy relation - extend succession chain`() {
        val savedA = createLegalEntity("$testName A")
        val savedB = createLegalEntity("$testName B")
        val savedC = createLegalEntity("$testName C")

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedB.legalEntity.header.bpnl
        )
        upsertRelationsGoldenRecordIntoPool("TASK_INITIAL", aReplacedByB)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = savedB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = savedC.legalEntity.header.bpnl
        )

        val result = upsertRelationsGoldenRecordIntoPool("TASK_CHAIN_EXTENSION", bReplacedByC).single()

        assertThat(result.errors).isEmpty()
        val fetchedB = poolClient.legalEntities.getLegalEntity(changeCase(savedB.legalEntity.header.bpnl))
        assertThat(fetchedB.header.relations.filter { it.type == LegalEntityRelationType.IsReplacedBy })
            .hasSize(2)
    }

    /**
     * GIVEN relation golden record task with unknown reason code
     * WHEN trying to refine relation task
     * THEN sharing process error returned
     */
    @Test
    fun `try create relation with unknown reason code`(){
        //GIVEN
        val legalEntity1 =  poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1)).entities.single()
        val legalEntity2 =  poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate2)).entities.single()

        val relation = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = legalEntity2.legalEntity.header.bpnl
        ).copy(reasonCode = "NOT EXISTS")

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("ANY ID", relation).single()

        //THEN
        assertThat(result.errors).isNotEmpty
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors.single().type).isEqualTo(TaskRelationsErrorType.Unspecified)
    }

    @ParameterizedTest
    @EnumSource(value = LegalEntityRelationType::class, names = ["IsReplacedBy"], mode = EnumSource.Mode.EXCLUDE)
    fun `reject unsupported address relation type`(relationType: LegalEntityRelationType) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        val createAddressRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = legalEntity1.legalEntity.legalAddress.bpna,
            businessPartnerTargetBpn = additionalAddress1.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).isEqualTo(
            "Invalid relation: source and target must be of the same business partner type and carry a relation type supported for it " +
                    "(source=${createAddressRelationsRequest.businessPartnerSourceBpn}, target=${createAddressRelationsRequest.businessPartnerTargetBpn}, " +
                    "relationType=${createAddressRelationsRequest.relationType})"
        )
    }

    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `reject relation between a legal entity and an address`(relationType: AddressRelationType) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)

        val createAddressRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = legalEntity1.legalEntity.header.bpnl,
            businessPartnerTargetBpn = additionalAddress1.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).isEqualTo(
            "Invalid relation: source and target must be of the same business partner type and carry a relation type supported for it " +
                    "(source=${createAddressRelationsRequest.businessPartnerSourceBpn}, target=${createAddressRelationsRequest.businessPartnerTargetBpn}, " +
                    "relationType=${createAddressRelationsRequest.relationType})"
        )
    }

    /**
     * GIVEN an address relation upsert request
     *   AND source address is of type AdditionalAddress
     *   AND target address is of type AdditionalAddress
     * WHEN trying to upsert an address relation
     * THEN validation exception is thrown indicating invalid source address type
     */
    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `reject address relation when source is not LegalAddress `(relationType: AddressRelationType) {
        //Given
        val legalEntity1 = createLegalEntity("$testName 1")
        val additionalAddress1 = createAdditionalAddress("$testName Addr 1", legalEntity1)
        val additionalAddress2 = createAdditionalAddress("$testName Addr 2", legalEntity1)

        val createAddressRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = additionalAddress1.address.bpna,
            businessPartnerTargetBpn = additionalAddress2.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).contains("Invalid source address type for 'IsReplacedBy' relation")
    }

    /**
     * GIVEN an address relation upsert request
     *   AND source address is of type LegalAddress associated with legal entity A
     *   AND target address is of type AdditionalAddress associated with legal entity B
     * WHEN trying to upsert an address relation
     * THEN validation exception is thrown indicating invalid address relation
     */
    @ParameterizedTest
    @EnumSource(AddressRelationType::class)
    fun `reject address relation when two different legal entities involved `(relationType: AddressRelationType) {
        //Given
        val legalEntityA = createLegalEntity("$testName A")
        val legalEntityB = createLegalEntity("$testName B")
        val additionalAddressB = createAdditionalAddress("$testName Addr 2", legalEntityB)

        val createAddressRelationsRequest = buildAlwaysActiveRelationRequest(
            relationType = relationType,
            businessPartnerSourceBpn = legalEntityA.legalEntity.legalAddress.bpna,
            businessPartnerTargetBpn = additionalAddressB.address.bpna
        )

        val result = upsertRelationsGoldenRecordIntoPool(taskId = "TASK_1", businessPartnerRelations = createAddressRelationsRequest)
        assertThat(result[0].taskId).isEqualTo("TASK_1")
        assertThat(result[0].errors.size).isEqualTo(1)
        assertThat(result[0].errors[0].description).contains("Invalid 'IsReplacedBy' relation:")
    }

    /**
     * GIVEN two sites of the same legal entity
     * WHEN creating relation 'site A is replaced by site B'
     * THEN the succession is accepted and reported back with both BPNS
     */
    @Test
    fun `create site IsReplacedBy relation`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val siteA = createSite("$testName A", legalEntity)
        val siteB = createSite("$testName B", legalEntity)

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteB.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_CREATE_SITE_SUCCESSION", aReplacedByB).single()

        //THEN
        assertThat(result.errors).isEmpty()
        assertThat(result.businessPartnerRelations.relationType).isEqualTo(RelationType.IsReplacedBy)
        assertThat(result.businessPartnerRelations.businessPartnerSourceBpn).isEqualTo(siteA.site.bpns)
        assertThat(result.businessPartnerRelations.businessPartnerTargetBpn).isEqualTo(siteB.site.bpns)
        assertThat(result.businessPartnerRelations.validityPeriods).isEqualTo(aReplacedByB.validityPeriods)
    }

    /**
     * GIVEN a site relation request whose source and target BPNS were never issued
     * WHEN trying to create the relation
     * THEN return not found error
     */
    @Test
    fun `create site IsReplacedBy relation - reject unknown sites`() {
        //GIVEN
        val unknownSiteRelation = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = "BPNS0000000000XY",
            businessPartnerTargetBpn = "BPNS0000000000ZY"
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_UNKNOWN_SITES", unknownSiteRelation).single()

        //THEN
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors[0].description).contains("Source site BPNS BPNS0000000000XY not found")
    }

    /**
     * GIVEN a site
     * WHEN trying to create relation 'the site is replaced by itself'
     * THEN return self reference error
     */
    @Test
    fun `create site IsReplacedBy relation - reject self reference`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val site = createSite("$testName A", legalEntity)

        val siteReplacedByItself = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = site.site.bpns,
            businessPartnerTargetBpn = site.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_SELF_REFERENCE", siteReplacedByItself).single()

        //THEN
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors[0].description).contains("A site cannot have a relation to itself")
    }

    /**
     * GIVEN site A of legal entity A and site B of legal entity B
     * WHEN trying to create relation 'site A is replaced by site B'
     * THEN return different legal entity error
     */
    @Test
    fun `create site IsReplacedBy relation - violate same legal entity`() {
        //GIVEN
        val legalEntityA = createLegalEntity("$testName LE A")
        val legalEntityB = createLegalEntity("$testName LE B")
        val siteA = createSite("$testName A", legalEntityA)
        val siteB = createSite("$testName B", legalEntityB)

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteB.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_SAME_LEGAL_ENTITY", aReplacedByB).single()

        //THEN
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors[0].description).contains("do not belong to the same Legal Entity")
    }

    /**
     * GIVEN site A is replaced by site B
     * WHEN trying to create relation 'site A is replaced by site C' in an overlapping validity period
     * THEN return multiple successors error
     */
    @Test
    fun `create site IsReplacedBy relation - violate single successor`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val siteA = createSite("$testName A", legalEntity)
        val siteB = createSite("$testName B", legalEntity)
        val siteC = createSite("$testName C", legalEntity)

        val validReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteB.site.bpns
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID", validReplacedByB)

        val violatingSingleSuccessor = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteC.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_SINGLE_SUCCESSOR", violatingSingleSuccessor).single()

        //THEN
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors[0].description).contains("Multiple successors assigned to the same site")
    }

    /**
     * GIVEN site A is replaced by site C
     * WHEN creating relation 'site B is replaced by site C' in an overlapping validity period
     * THEN the merger of both predecessors into one successor is accepted
     */
    @Test
    fun `create site IsReplacedBy relation - allow merger into a shared successor`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val siteA = createSite("$testName A", legalEntity)
        val siteB = createSite("$testName B", legalEntity)
        val siteC = createSite("$testName C", legalEntity)

        val aReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteC.site.bpns
        )
        upsertRelationsGoldenRecordIntoPool("TASK_FIRST_PREDECESSOR", aReplacedByC)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteB.site.bpns,
            businessPartnerTargetBpn = siteC.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_SECOND_PREDECESSOR", bReplacedByC).single()

        //THEN
        assertThat(result.errors).isEmpty()
    }

    /**
     * GIVEN site A is replaced by site B which is replaced by site C
     * WHEN trying to create relation 'site C is replaced by site A'
     * THEN return circular replacement error
     */
    @Test
    fun `create site IsReplacedBy relation - violate no cycles`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val siteA = createSite("$testName A", legalEntity)
        val siteB = createSite("$testName B", legalEntity)
        val siteC = createSite("$testName C", legalEntity)

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteB.site.bpns
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_1", aReplacedByB)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteB.site.bpns,
            businessPartnerTargetBpn = siteC.site.bpns
        )
        upsertRelationsGoldenRecordIntoPool("TASK_VALID_2", bReplacedByC)

        val violatingNoCycles = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteC.site.bpns,
            businessPartnerTargetBpn = siteA.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_VIOLATE_NO_CYCLES", violatingNoCycles).single()

        //THEN
        assertThat(result.errors.size).isEqualTo(1)
        assertThat(result.errors[0].description).contains("Circular replacement detected")
    }

    /**
     * GIVEN site A is replaced by site B
     * WHEN creating relation 'site B is replaced by site C'
     * THEN the succession chain is accepted and both relations are reported on B
     */
    @Test
    fun `create site IsReplacedBy relation - extend succession chain`() {
        //GIVEN
        val legalEntity = createLegalEntity(testName)
        val siteA = createSite("$testName A", legalEntity)
        val siteB = createSite("$testName B", legalEntity)
        val siteC = createSite("$testName C", legalEntity)

        val aReplacedByB = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteA.site.bpns,
            businessPartnerTargetBpn = siteB.site.bpns
        )
        upsertRelationsGoldenRecordIntoPool("TASK_INITIAL", aReplacedByB)

        val bReplacedByC = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = siteB.site.bpns,
            businessPartnerTargetBpn = siteC.site.bpns
        )

        //WHEN
        val result = upsertRelationsGoldenRecordIntoPool("TASK_CHAIN_EXTENSION", bReplacedByC).single()

        //THEN
        assertThat(result.errors).isEmpty()
        val fetchedB = poolClient.sites.getSite(siteB.site.bpns)
        assertThat(fetchedB.site.relations.filter { it.type == SiteRelationType.IsReplacedBy })
            .hasSize(2)
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'B is alternative headquarter of A'
     * THEN return star topology error because A is already an alternative (target cannot be alternative)
     */
    @Test
    fun `create IsAlternativeHeadquarterFor relation - reject target already alternative`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")
        val otherB = createLegalEntity("$testName B")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_A_TO_M", aToM)[0].errors).isEmpty()

        val bToA = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = otherB.legalEntity.header.bpnl,
            businessPartnerTargetBpn = alternativeA.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_B_TO_A", bToA)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("Star topology violated")
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'M is alternative headquarter of D'
     * THEN return star topology error because M is already a main (source cannot be main)
     */
    @Test
    fun `create IsAlternativeHeadquarterFor relation - reject source already main`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")
        val otherD = createLegalEntity("$testName D")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_A_TO_M", aToM)[0].errors).isEmpty()

        val mToD = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = mainM.legalEntity.header.bpnl,
            businessPartnerTargetBpn = otherD.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_M_TO_D", mToD)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("Star topology violated")
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'A is alternative headquarter of D'
     * THEN return star topology error because A is already alternative to M
     */
    @Test
    fun `create IsAlternativeHeadquarterFor relation - reject source already alternative of different main`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")
        val otherD = createLegalEntity("$testName D")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_A_TO_M", aToM)[0].errors).isEmpty()

        val aToD = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = otherD.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_A_TO_D", aToD)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("Star topology violated")
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'M is alternative headquarter of A'
     * THEN return reverse relation error
     */
    @Test
    fun `create IsAlternativeHeadquarterFor relation - reject reverse pair`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_A_TO_M", aToM)[0].errors).isEmpty()

        val mToA = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = mainM.legalEntity.header.bpnl,
            businessPartnerTargetBpn = alternativeA.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_M_TO_A", mToA)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("Cannot reverse alternative headquarter relation")
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'A is owned by M'
     * THEN return error because alternative cannot participate in IsOwnedBy
     */
    @Test
    fun `create IsOwnedBy relation - reject alternative as source`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_ALT", aToM)[0].errors).isEmpty()

        val aOwnedByM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_OWNED", aOwnedByM)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("cannot participate in ownership")
    }

    /**
     * GIVEN legal entity A is alternative headquarter of M
     * WHEN trying to create relation 'M is owned by A'
     * THEN return error because A is alternative and cannot be an owner
     */
    @Test
    fun `create IsOwnedBy relation - reject alternative as target`() {
        val alternativeA = createLegalEntity("$testName A")
        val mainM = createLegalEntity("$testName M")

        val aToM = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsAlternativeHeadquarterFor,
            businessPartnerSourceBpn = alternativeA.legalEntity.header.bpnl,
            businessPartnerTargetBpn = mainM.legalEntity.header.bpnl
        )
        assertThat(upsertRelationsGoldenRecordIntoPool("TASK_ALT", aToM)[0].errors).isEmpty()

        val mOwnedByA = buildAlwaysActiveRelationRequest(
            relationType = RelationType.IsOwnedBy,
            businessPartnerSourceBpn = mainM.legalEntity.header.bpnl,
            businessPartnerTargetBpn = alternativeA.legalEntity.header.bpnl
        )
        val result = upsertRelationsGoldenRecordIntoPool("TASK_OWNED", mOwnedByA)[0]
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.first().description).contains("cannot participate in ownership")
    }


    private fun createLegalEntity(seed: String): LegalEntityPartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.createLegalEntityRequest(seed, true)
        return poolClient.legalEntities.createBusinessPartners(listOf(request)).entities.single()
    }

    private fun createAdditionalAddress(seed: String, legalEntity: LegalEntityPartnerCreateVerboseDto): AddressPartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.buildAdditionalAddressCreateRequest(seed, legalEntity.legalEntity.header.bpnl)
        return poolClient.addresses.createAddresses(listOf(request)).entities.single()
    }

    private fun createSite(seed: String, legalEntity: LegalEntityPartnerCreateVerboseDto): SitePartnerCreateVerboseDto {
        val request = testDataEnvironment.requestFactory.buildSiteCreateRequest(seed, legalEntity.legalEntity.header.bpnl)
        return poolClient.sites.createSite(listOf(request)).entities.single()
    }

    private fun upsertRelationsGoldenRecordIntoPool(taskId: String, businessPartnerRelations: BusinessPartnerRelations): List<TaskRelationsStepResultEntryDto> {

        val taskStep = singleTaskStep(taskId = taskId, businessPartnerRelations = businessPartnerRelations)
        return taskRelationsResolutionService.upsertRelationsGoldenRecordIntoPool(taskStep)
    }

    private fun LegalEntityRelationType.toTaskDto(): RelationType {
        return when(this){
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> RelationType.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> RelationType.IsOwnedBy
            LegalEntityRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }
    }

    private fun AddressRelationType.toTaskDto(): RelationType {
        return when(this){
            AddressRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }
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


    private fun buildAlwaysActiveRelationRequest(
        relationType: RelationType,
        businessPartnerSourceBpn: String,
        businessPartnerTargetBpn: String
    ): BusinessPartnerRelations{
        return buildRelationRequest(
            relationType,
            businessPartnerSourceBpn,
            businessPartnerTargetBpn,
            listOf(
                RelationValidityPeriod(
                    validFrom = LocalDate.of(1970, 1, 1),
                    validTo = LocalDate.of(9999, 12, 31)
                )
            )
        )
    }

    private fun buildAlwaysActiveRelationRequest(
        relationType: LegalEntityRelationType,
        businessPartnerSourceBpn: String,
        businessPartnerTargetBpn: String
    ): BusinessPartnerRelations{
        return buildAlwaysActiveRelationRequest(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = businessPartnerSourceBpn,
            businessPartnerTargetBpn = businessPartnerTargetBpn
        )
    }

    private fun buildAlwaysActiveRelationRequest(
        relationType: AddressRelationType,
        businessPartnerSourceBpn: String,
        businessPartnerTargetBpn: String
    ): BusinessPartnerRelations{
        return buildAlwaysActiveRelationRequest(
            relationType = relationType.toTaskDto(),
            businessPartnerSourceBpn = businessPartnerSourceBpn,
            businessPartnerTargetBpn = businessPartnerTargetBpn
        )
    }

    private fun buildRelationRequest(
        relationType: RelationType,
        businessPartnerSourceBpn: String,
        businessPartnerTargetBpn: String,
        validityPeriods: List<RelationValidityPeriod>
    ): BusinessPartnerRelations{
        return BusinessPartnerRelations(
            relationType = relationType,
            businessPartnerSourceBpn = businessPartnerSourceBpn,
            businessPartnerTargetBpn = businessPartnerTargetBpn,
            validityPeriods = validityPeriods,
            testDataEnvironment.metadata.reasonCodes.first().technicalKey
        )
    }

}