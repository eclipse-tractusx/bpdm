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

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as GateRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.GoldenRecordRelationAssertHelper
import org.eclipse.tractusx.bpdm.test.system.utils.RelationState
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.eclipse.tractusx.bpdm.test.system.utils.TaskReservationWatcher
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withExternalId
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withSource
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withTarget
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withValidityPeriods
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

/**
 * Steps for the "Output Reflects Golden Record Relations" feature.
 *
 * The golden records the relation connects are set up by the reused "Given record reflects ..." steps. The
 * relation itself is shared as a Gate relation input and then "established" by the test acting as the
 * cleaning service: it resolves the relation golden record task with the two golden records' BPNs, choosing
 * the legal entity BPN for legal-entity relations and the address BPN for address relations. Validity is set
 * to currently valid for the types that require it (IsManagedBy must not start in the past; IsReplacedBy
 * must be currently valid). The Then steps then poll each record's output until it reflects the relation,
 * because the relation propagates into already-shared outputs asynchronously.
 */
class GoldenRecordRelationsInOutputStepDefs(
    private val gateClient: GateClient,
    private val orchestratorClient: OrchestrationApiClient,
    private val shareActions: BusinessPartnerShareActions,
    private val sharingStateWatcher: SharingStateWatcher,
    private val taskReservationWatcher: TaskReservationWatcher,
    private val testDataFactoryGate: TestDataFactoryGateV7,
    private val assertHelper: GoldenRecordRelationAssertHelper,
    private val jsonMapper: JsonMapper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }

        private val ADDRESS_RELATION_TYPES = setOf(GateRelationType.IsReplacedBy)
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    // The relation scenarios do not care about the records' master data, so these Given steps omit it and the
    // implementation derives a master data seed from the entity label. All records are shared as own company
    // data so their legal entities become dataspace participants (required by an IsManagedBy managing entity);
    // the "own company record" wording makes that explicit where a scenario relies on it.

    @Given("record {string} reflects legal entity {string}")
    fun `given record reflects legal entity`(recordId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Given: record '$recordId' reflects legal entity '$legalEntityId'" }
        shareAndRefineLegalEntity(recordId, legalEntityId)
    }

    @Given("own company record {string} reflects legal entity {string}")
    fun `given own company record reflects legal entity`(recordId: String, legalEntityId: String) {
        logger.info { "[$scenarioName] Given: own company record '$recordId' reflects legal entity '$legalEntityId'" }
        shareAndRefineLegalEntity(recordId, legalEntityId)
    }

    @Given("record {string} reflects additional address {string} of legal entity {string}")
    fun `given record reflects additional address of legal entity`(recordId: String, addressId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of legal entity '$legalEntityId'"
        }
        shareActions.upload(recordId, isOwnCompanyData = true)
        val addressWithParent = shareActions.refineAsAdditionalAddressOfLegalEntity(
            recordId,
            masterDataSeed = addressId,
            additionalAddressLabel = addressId,
            legalEntityLabel = legalEntityId
        )
        context.legalEntities[legalEntityId] = addressWithParent.legalEntity
        context.additionalLegalEntityAddresses[addressId] = addressWithParent
    }

    @Given("record {string} reflects additional address {string} of the existing legal entity {string}")
    fun `given record reflects additional address of existing legal entity`(recordId: String, addressId: String, legalEntityId: String) {
        logger.info {
            "[$scenarioName] Given: record '$recordId' reflects additional address '$addressId' of the existing " +
                "legal entity '$legalEntityId'"
        }
        // Attach the additional address to the legal entity golden record an earlier step already refined, so
        // both addresses share one legal entity - required for an IsReplacedBy relation between them.
        shareActions.upload(recordId, isOwnCompanyData = true)
        shareActions.refineAsAdditionalAddressOfExistingLegalEntity(
            recordId,
            masterDataSeed = addressId,
            additionalAddressLabel = addressId,
            parentLegalEntityLabel = legalEntityId,
            scriptCode = "CHINESE_SIMPLIFIED"
        )
    }

    private fun shareAndRefineLegalEntity(recordId: String, legalEntityId: String) {
        shareActions.upload(recordId, isOwnCompanyData = true)
        val legalEntity = shareActions.refineAsLegalEntity(recordId, masterDataSeed = legalEntityId, legalEntityLabel = legalEntityId)
        context.legalEntities[legalEntityId] = legalEntity
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("the sharing member shares relation {string} of type {string} from {string} to {string}")
    fun `when shares relation`(relationId: String, relationType: String, sourceRecordId: String, targetRecordId: String) {
        logger.info {
            "[$scenarioName] When: the sharing member shares relation '$relationId' of type '$relationType' " +
                "from '$sourceRecordId' to '$targetRecordId'"
        }
        shareRelation(relationId, relationType, sourceRecordId, targetRecordId, currentlyValid = false)
    }

    @When("the sharing member shares relation {string} of type {string} from {string} to {string} with validity starting now and not in the past")
    fun `when shares relation valid from now`(relationId: String, relationType: String, sourceRecordId: String, targetRecordId: String) {
        logger.info {
            "[$scenarioName] When: the sharing member shares relation '$relationId' of type '$relationType' " +
                "from '$sourceRecordId' to '$targetRecordId' with validity starting now"
        }
        // IsManagedBy validity must not start in the past, so the relation is shared currently valid (today).
        shareRelation(relationId, relationType, sourceRecordId, targetRecordId, currentlyValid = true)
    }

    @When("the sharing member shares relation {string} of type {string} from {string} to {string} effective immediately")
    fun `when shares relation effective immediately`(relationId: String, relationType: String, sourceRecordId: String, targetRecordId: String) {
        logger.info {
            "[$scenarioName] When: the sharing member shares relation '$relationId' of type '$relationType' " +
                "from '$sourceRecordId' to '$targetRecordId' effective immediately"
        }
        // IsReplacedBy must be currently valid, so the relation is shared effective immediately (today).
        shareRelation(relationId, relationType, sourceRecordId, targetRecordId, currentlyValid = true)
    }

    private fun shareRelation(relationId: String, relationType: String, sourceRecordId: String, targetRecordId: String, currentlyValid: Boolean) {
        val baseEntry = testDataFactoryGate.relation.input.request
            .fromSeed("$relationId${context.scenarioSuffix}")
            .withExternalId(context.runId(relationId))
            .withRelationType(GateRelationType.valueOf(relationType))
            .withSource(context.runId(sourceRecordId))
            .withTarget(context.runId(targetRecordId))
        val entry = if (currentlyValid)
            baseEntry.withValidityPeriods(listOf(RelationValidityPeriodDto(validFrom = LocalDate.now(), validTo = null)))
        else
            baseEntry

        val request = RelationPutRequest(listOf(entry))
        val response = gateClient.relation.put(true, request)
        attachGateCall("PUT", "/v7/input/relations", request = request, response = response)
        context.relations[relationId] = RelationState(
            submittedEntry = entry,
            sourceRecordId = sourceRecordId,
            targetRecordId = targetRecordId
        )
    }

    @When("the golden record process establishes relation {string}")
    fun `when establishes relation`(relationId: String) {
        logger.info { "[$scenarioName] When: the golden record process establishes relation '$relationId'" }
        val relationState = context.relations[relationId]!!
        val runId = context.runId(relationId)

        sharingStateWatcher.waitForRelationTaskId(relationId)
        val relationSharingStatePage = gateClient.relationSharingState.get(
            externalIds = listOf(runId), sharingStateTypes = null, updatedAfter = null, paginationRequest = PaginationRequest()
        )
        attachGateCall("GET", "/v7/relations/sharing-state", request = mapOf("externalIds" to listOf(runId)), response = relationSharingStatePage)
        val taskId = relationSharingStatePage.content.single().taskId!!
        taskReservationWatcher.waitForReservedRelationTask(taskId)

        // The relation connects golden records, so resolve it with the matched golden records' BPNs: the
        // legal entity BPN for legal-entity relations and the address BPN for address relations.
        val isAddressRelation = relationState.submittedEntry.relationType in ADDRESS_RELATION_TYPES
        val sourceBpn = goldenRecordBpnOf(relationState.sourceRecordId, isAddressRelation)
        val targetBpn = goldenRecordBpnOf(relationState.targetRecordId, isAddressRelation)

        orchestratorClient.relationsGoldenRecordTasks.resolveStepResults(
            TaskRelationsStepResultRequest(TaskStep.CleanAndSync, listOf(
                TaskRelationsStepResultEntryDto(taskId, BusinessPartnerRelations(
                    relationType = OrchestratorRelationType.valueOf(relationState.submittedEntry.relationType.name),
                    businessPartnerSourceBpn = sourceBpn,
                    businessPartnerTargetBpn = targetBpn,
                    validityPeriods = relationState.submittedEntry.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
                    reasonCode = relationState.submittedEntry.reasonCode
                ))
            ))
        )

        sharingStateWatcher.waitForRelationCompletedState(relationId)
        context.relations[relationId] = relationState.copy(resolvedSourceBpn = sourceBpn, resolvedTargetBpn = targetBpn)
    }

    // -------------------------------------------------------------------------
    // Then
    // -------------------------------------------------------------------------

    @Then("{string} output reflects the legal entity golden record relation {string}")
    fun `then output reflects legal entity relation`(recordId: String, relationId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the legal entity golden record relation '$relationId'" }
        assertHelper.assertLegalEntityRelationReflected(recordId, context.relations[relationId]!!)
    }

    @Then("{string} output reflects the legal entity golden record relation {string} through its parent legal entity {string}")
    fun `then output reflects parent legal entity relation`(recordId: String, relationId: String, parentLegalEntityId: String) {
        logger.info {
            "[$scenarioName] Then: '$recordId' output reflects the legal entity golden record relation '$relationId' " +
                "through its parent legal entity '$parentLegalEntityId'"
        }
        val parentBpnl = context.legalEntities[parentLegalEntityId]?.header?.bpnl
            ?: error("legal entity '$parentLegalEntityId' must be defined by an earlier golden record refinement step")
        assertHelper.assertLegalEntityRelationReflected(recordId, context.relations[relationId]!!, expectedParentBpnl = parentBpnl)
    }

    @Then("{string} output reflects the address golden record relation {string}")
    fun `then output reflects address relation`(recordId: String, relationId: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects the address golden record relation '$relationId'" }
        assertHelper.assertAddressRelationReflected(recordId, context.relations[relationId]!!)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun goldenRecordBpnOf(recordId: String, isAddressRelation: Boolean): String {
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachGateCall("POST", "/v7/output/business-partners/search", request = listOf(runId), response = outputPage)
        val output = outputPage.content.single()
        return if (isAddressRelation) output.address.addressBpn else output.legalEntity.legalEntityBpn
    }

    private fun attachGateCall(method: String, path: String, request: Any? = null, response: Any? = null) {
        val content = buildMap {
            put("uri", "$method $path")
            if (request != null) put("request", request)
            if (response != null) put("response", response)
        }
        context.scenario.attach(
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content),
            "application/json",
            "$method $path"
        )
    }
}
