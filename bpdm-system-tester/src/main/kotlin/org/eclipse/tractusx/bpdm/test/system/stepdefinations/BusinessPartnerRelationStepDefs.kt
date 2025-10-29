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

package org.eclipse.tractusx.bpdm.test.system.stepdefinations

import io.cucumber.java.BeforeStep
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationOutputSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.pool.api.model.RelationVerboseDto
import org.eclipse.tractusx.bpdm.test.system.utils.StepUtils
import org.eclipse.tractusx.bpdm.test.system.utils.TestRepository
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.withAddressType
import org.eclipse.tractusx.bpdm.test.testdata.gate.withoutAnyBpn
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType as PoolRelationType


class BusinessPartnerRelationStepDefs(
    private val stepUtils: StepUtils,
    private val inputFactory: GateInputFactory,
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val poolApiClient: PoolApiClient,
    private val testRepository: TestRepository
): SpringTestRunConfiguration() {

    private val anyTime: Instant = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()

    private val defaultRelationStates = listOf(RelationValidityPeriodDto(
        validFrom = LocalDate.parse("1970-01-01"),
        validTo = LocalDate.parse("9999-12-31")
    ))

    private val defaultPoolRelationStates = defaultRelationStates.map {
        RelationValidityPeriod(it.validFrom, it.validTo)
    }

    /**
     * Since BPNs are created on-the-fly by the Pool we can't assign a certain BPN directly to a shared record
     * Therefore we associate the BPN tag in the Gherkin description with the BPN after it has been generated
     * In the following Cucumber steps we can then reference the BPN by the BPN tag in the Gherkin description
     */
    private val bpnsByTag = mutableMapOf<String, String>()
    private val externalIdsByBpnTag = mutableMapOf<String, String>()

    private lateinit var scenario: Scenario

    @BeforeStep
    fun initializeScenario(scenario: Scenario){
        this.scenario = scenario
    }

    /**
     * Creates a business partner of type legal entity in the Gate and shares it with the Pool
     *
     * The created BPNL is stored by the given BPN tag in the Gherkin description
     */
    @Given("shared legal entity with external-ID {string} and BPNL {string}")
    fun `given shared legal entity`(externalId: String, bpnTag: String) {
        val externalId = externalId.toScenarioInstance()
        val bpnTag = bpnTag.toScenarioInstance()

        val legalEntityInputRequest = inputFactory.createFullValid(externalId, withTestRunContext = false)
            .withAddressType(AddressType.LegalAndSiteMainAddress)
            .withoutAnyBpn()

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(legalEntityInputRequest))
        externalIdsByBpnTag.put(bpnTag, externalId)
    }

    /**
     * Creates a business partner of type legal entity in the Gate and shares it with the Pool
     *
     * Waits for sharing process to finish then created BPNL is stored by the given BPN tag in the Gherkin description
     */
    @Given("shared legal entity with external-ID {string} and BPNL {string} finished sharing")
    fun `given shared legal entity finished sharing`(externalId: String, bpnTag: String) {
        val extId = externalId.toScenarioInstance()
        val tag = bpnTag.toScenarioInstance()

        val legalEntityInputRequest = inputFactory.createFullValid(extId, withTestRunContext = false)
            .withAddressType(AddressType.LegalAndSiteMainAddress)
            .withoutAnyBpn()

        gateClient.businessParters.upsertBusinessPartnersInput(listOf(legalEntityInputRequest))

        // Wait explicitly for sharing to complete
        stepUtils.waitForBusinessPartnerResult(extId)

        val legalEntityOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(extId)).content.single()
        val bpn = legalEntityOutput.legalEntity.legalEntityBpn

        externalIdsByBpnTag[tag] = extId
        bpnsByTag[tag] = bpn
    }

    /**
     * Creates a relation new relation and shares it with the pool
     *
     */
    @Given("shared relation with external-ID {string} of type {string}, source {string} and target {string}")
    fun `given shared relation`(
        relationExternalId: String,
        relationTypeString: String,
        sourceExternalId: String,
        targetExternalId: String
    ) {
        val relationExternalId = relationExternalId.toScenarioInstance()
        val relationType = GateRelationType.valueOf(relationTypeString)
        val sourceExternalId = sourceExternalId.toScenarioInstance()
        val targetExternalId = targetExternalId.toScenarioInstance()

        shareRelation(relationExternalId, relationType, sourceExternalId, targetExternalId)
    }


    /**
     * A relation is created in the Gate and passed through the refinement step in the golden record process
     *
     * The relation content is not changed in the refinement step
     */
    @When("sharing relation with external-ID {string} of type {string}, source {string} and target {string}")
    fun `when sharing relation`(externalId: String, relationTypeString: String, source: String, target: String) {
        val externalId = externalId.toScenarioInstance()
        val source = source.toScenarioInstance()
        val target = target.toScenarioInstance()
        val relationType = GateRelationType.valueOf(relationTypeString)

        shareRelation(externalId, relationType, source, target)
        stepUtils.waitForRelationResult(externalId)
    }

    /**
     * Check whether the Pool's golden record legal entities have the specified relation information
     *
     * Check both legal entities that are referenced in the source and target of the relation
     */
    @Then("Pool has relation of type {string}, source {string} and target {string}")
    fun `then pool has relation`(relationTypeString: String, sourceBpnTag: String, targetBpnTag: String) {
        val sourceBpnTag = sourceBpnTag.toScenarioInstance()
        val targetBpnTag = targetBpnTag.toScenarioInstance()
        val relationType = PoolRelationType.valueOf(relationTypeString)
        val sourceBpn = getBpnL(sourceBpnTag)
        val targetBpn = getBpnL(targetBpnTag)

        val sourceLegalEntity = poolApiClient.legalEntities.getLegalEntity(sourceBpn)
        val targetLegalEntity = poolApiClient.legalEntities.getLegalEntity(targetBpn)

        val expectedRelation = RelationVerboseDto(
            type = relationType,
            businessPartnerSourceBpnl = sourceBpn,
            businessPartnerTargetBpnl = targetBpn,
            validityPeriods = defaultPoolRelationStates
        )

        val sourceRelations = sourceLegalEntity.legalEntity.relations
        val targetRelations = targetLegalEntity.legalEntity.relations

        Assertions.assertThat(sourceRelations).contains(expectedRelation)
        Assertions.assertThat(targetRelations).contains(expectedRelation)
    }

    /**
     * Check Gate has the specified relation output
     */
    @Then("Gate has relation output with external-ID {string} of type of type {string}, source {string} and target {string}")
    fun `then gate has relation output`(externalId: String, relationTypeString: String, sourceBpnTag: String, targetBpnTag: String) {
        val externalId = externalId.toScenarioInstance()
        val sourceBpnTag = sourceBpnTag.toScenarioInstance()
        val targetBpnTag = targetBpnTag.toScenarioInstance()
        val relationType = SharableRelationType.valueOf(relationTypeString)
        val sourceBpn = getBpnL(sourceBpnTag)
        val targetBpn = getBpnL(targetBpnTag)

        val relationOutput = gateClient.relationOutput.postSearch(RelationOutputSearchRequest(externalIds = listOf(externalId))).content.single()

        val expectedRelation = RelationOutputDto(
            externalId = externalId,
            relationType = relationType,
            sourceBpnL = sourceBpn,
            targetBpnL = targetBpn,
            validityPeriods = defaultRelationStates,
            updatedAt = anyTime,
        )

        Assertions.assertThat(relationOutput)
            .usingRecursiveComparison()
            .ignoringFields(RelationOutputDto::updatedAt.name)
            .isEqualTo(expectedRelation)
    }
    /**
     * Check Gate has changelog output entry of given type
     */
    @Then("Gate has relation changelog entry with external-ID {string} with type {string}")
    fun `then gate has output changelog entry`(externalId: String, changelogTypeString: String) {
        val externalId = externalId.toScenarioInstance()
        val changelogType = ChangelogType.valueOf(changelogTypeString)

        val changelogResponse = gateClient.relationChangelog.getOutputChangelog(PaginationRequest(), ChangelogSearchRequest(externalIds = setOf(externalId)))

        assert(changelogResponse.content.any{ it.changelogType == changelogType })
    }

    private fun shareRelation(
        relationExternalId: String,
        relationType: GateRelationType,
        sourceExternalId: String,
        targetExternalId: String
    ){
        val relationInputRequest = RelationPutEntry(relationExternalId, relationType, sourceExternalId, targetExternalId, defaultRelationStates)
        gateClient.relation.put(true, RelationPutRequest(listOf(relationInputRequest)))
        val taskId = stepUtils.waitForRelationTask(relationExternalId)

        testRepository.reserveTasks()
        resolveReservedTask(taskId)
    }

    /**
     * Resolves without changing the relation content
     */
    private fun resolveReservedTask(taskId: String){
        val reservedTask = testRepository.getReservedTask(taskId)

        val refinementResult = TaskRelationsStepResultEntryDto(reservedTask.taskId, reservedTask.businessPartnerRelations, emptyList())
        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(TaskStep.CleanAndSync, listOf(refinementResult)))
    }

    private fun String.toScenarioInstance() = "${scenario.name}-$this"

    private fun getBpnL(bpnTag: String): String{
        if(!bpnsByTag.contains(bpnTag)){
                val externalId = externalIdsByBpnTag[bpnTag]!!
                stepUtils.waitForBusinessPartnerResult(externalId)
                val legalEntityOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId)).content.single()
                bpnsByTag.put(bpnTag,  legalEntityOutput.legalEntity.legalEntityBpn)
        }

        return bpnsByTag[bpnTag]!!
    }

}