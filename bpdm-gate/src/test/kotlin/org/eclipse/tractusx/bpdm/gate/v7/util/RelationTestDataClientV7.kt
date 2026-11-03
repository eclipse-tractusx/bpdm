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

package org.eclipse.tractusx.bpdm.gate.v7.util

import org.eclipse.tractusx.bpdm.gate.api.v7.client.GateV7Client
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.service.application.RelationTaskCreationApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.RelationTaskResolutionApplicationService
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskClientRelationsStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType

class RelationTestDataClientV7(
    private val gateClient: GateV7Client,
    private val testDataFactory: TestDataFactoryGateV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val relationTaskCreationService: RelationTaskCreationApplicationService,
    private val relationTaskResolutionService: RelationTaskResolutionApplicationService,
    private val businessPartner: BusinessPartnerTestDataClientV7,
) {

    fun upsertRelationInput(entry: RelationPutV7Entry, createIfNotExist: Boolean = true): RelationV7Dto {
        return gateClient.relation.put(createIfNotExist, RelationPutV7Request(listOf(entry))).upsertedRelations.single()
    }

    fun upsertRelationInputWithBusinessPartners(entry: RelationPutV7Entry, createIfNotExist: Boolean = true): RelationV7Dto {
        businessPartner.upsertInput(entry.businessPartnerSourceExternalId)
        businessPartner.upsertInput(entry.businessPartnerTargetExternalId)
        return upsertRelationInput(entry, createIfNotExist)
    }

    fun upsertRelationInputWithBusinessPartners(seed: String, relationType: RelationTypeV7): RelationV7Dto {
        return upsertRelationInputWithBusinessPartners(
            testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        )
    }

    fun setRelationStateToPending(externalId: String, seed: String = externalId): TaskClientRelationsStateDto {
        val createdTask = orchestratorMockDataFactory.mockCreateRelationTask(seed)
        relationTaskCreationService.createTasksForReadyRelations()
        return createdTask
    }

    fun setRelationStateToSuccess(externalId: String, seed: String = externalId): TaskClientRelationsStateDto {
        val refinedTask = orchestratorMockDataFactory.mockRefineRelation(seed)
        relationTaskCreationService.createTasksForReadyRelations()
        relationTaskResolutionService.resolveFinishedTasks()
        return refinedTask
    }

    fun setRelationStateToError(externalId: String, seed: String = externalId, errorType: TaskRelationsErrorType): TaskClientRelationsStateDto {
        val errorTask = orchestratorMockDataFactory.mockRelationSharingError(seed, errorType)
        relationTaskCreationService.createTasksForReadyRelations()
        relationTaskResolutionService.resolveFinishedTasks()
        return errorTask
    }

    fun createRelationWithOutputs(
        seed: String,
        relationType: RelationTypeV7,
        sourceRefine: (BusinessPartnerInputV7Dto) -> Unit,
        targetRefine: (BusinessPartnerInputV7Dto) -> Unit
    ): RelationV7Dto {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).copy(relationType = relationType)
        val relation = upsertRelationInput(request)
        sourceRefine(source)
        targetRefine(target)
        return relation
    }

    fun createRelationInputWithRefinedLegalEntityBPs(seed: String, relationType: RelationTypeV7 = RelationTypeV7.IsManagedBy): RelationV7Dto {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntity(source)
        businessPartner.refineToLegalEntity(target)
        return relationInput
    }

    fun createLegalEntityRelationOutput(seed: String, relationType: RelationTypeV7 = RelationTypeV7.IsManagedBy): Pair<RelationV7Dto, BusinessPartnerRelations> {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntity(source)
        businessPartner.refineToLegalEntity(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun createAddressRelationOutput(seed: String, relationType: RelationTypeV7 = RelationTypeV7.IsReplacedBy): Pair<RelationV7Dto, BusinessPartnerRelations> {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntityOnSite(source)
        businessPartner.refineToAdditionalAddressOfSite(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun updateLegalEntityRelationOutput(original: RelationV7Dto, updateSeed: String, relationType: RelationTypeV7): BusinessPartnerRelations {
        val newSource = businessPartner.upsertInput("$updateSeed Source")
        val newTarget = businessPartner.upsertInput("$updateSeed Target")
        val updatedRequest = testDataFactory.relation.input.request.fromSeed(updateSeed).copy(
            externalId = original.externalId,
            relationType = relationType
        )
        val updatedRelationInput = upsertRelationInput(updatedRequest)
        businessPartner.refineToLegalEntity(newSource)
        businessPartner.refineToLegalEntity(newTarget)
        return refineRelationToSuccess(updatedRelationInput, updateSeed)
    }

    fun refineRelationToSuccess(input: RelationV7Dto, seed: String = input.externalId): BusinessPartnerRelations {
        val refinementResult = orchestratorMockDataFactory.mockRefineRelation(seed).businessPartnerRelationsResult
        relationTaskCreationService.createTasksForReadyRelations()
        relationTaskResolutionService.resolveFinishedTasks()
        return refinementResult
    }
}
