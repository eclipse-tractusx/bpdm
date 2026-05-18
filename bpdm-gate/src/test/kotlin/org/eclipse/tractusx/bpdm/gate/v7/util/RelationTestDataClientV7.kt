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

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskCreationService
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskResolutionService
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.TaskClientRelationsStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType

class RelationTestDataClientV7(
    private val gateClient: GateClient,
    private val testDataFactory: TestDataFactoryGateV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val relationTaskCreationService: RelationTaskCreationService,
    private val relationTaskResolutionService: RelationTaskResolutionService,
    private val businessPartner: BusinessPartnerTestDataClientV7,
) {

    fun upsertRelationInput(entry: RelationPutEntry, createIfNotExist: Boolean = true): RelationDto {
        return gateClient.relation.put(createIfNotExist, RelationPutRequest(listOf(entry))).upsertedRelations.single()
    }

    fun upsertRelationInput(externalId: String, source: BusinessPartnerInputDto, target: BusinessPartnerInputDto, seed: String = externalId): RelationDto {
        val request = testDataFactory.relation.input.request.fromSeed(seed).copy(
            businessPartnerSourceExternalId = source.externalId,
            businessPartnerTargetExternalId = target.externalId
        )
        return gateClient.relation.put(true, RelationPutRequest(listOf(request))).upsertedRelations.single()
    }

    fun upsertRelationInputWithBusinessPartners(entry: RelationPutEntry, createIfNotExist: Boolean = true): RelationDto {
        businessPartner.upsertInput(entry.businessPartnerSourceExternalId)
        businessPartner.upsertInput(entry.businessPartnerTargetExternalId)
        return upsertRelationInput(entry, createIfNotExist)
    }

    fun upsertRelationInputWithBusinessPartners(seed: String, relationType: RelationType): RelationDto {
        return upsertRelationInputWithBusinessPartners(
            testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        )
    }

    fun setRelationStateToPending(externalId: String, seed: String = externalId): TaskClientRelationsStateDto {
        val createdTask = orchestratorMockDataFactory.mockCreateRelationTask(seed)
        relationTaskCreationService.sendTasks()
        return createdTask
    }

    fun setRelationStateToSuccess(externalId: String, seed: String = externalId): TaskClientRelationsStateDto {
        val refinedTask = orchestratorMockDataFactory.mockRefineRelation(seed)
        relationTaskCreationService.sendTasks()
        relationTaskResolutionService.checkResolveTasks()
        return refinedTask
    }

    fun setRelationStateToError(externalId: String, seed: String = externalId, errorType: TaskRelationsErrorType): TaskClientRelationsStateDto {
        val errorTask = orchestratorMockDataFactory.mockRelationSharingError(seed, errorType)
        relationTaskCreationService.sendTasks()
        relationTaskResolutionService.checkResolveTasks()
        return errorTask
    }

    fun createRelationWithOutputs(
        seed: String,
        relationType: RelationType,
        sourceRefine: (BusinessPartnerInputDto) -> Unit,
        targetRefine: (BusinessPartnerInputDto) -> Unit
    ): RelationDto {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).copy(relationType = relationType)
        val relation = upsertRelationInput(request)
        sourceRefine(source)
        targetRefine(target)
        return relation
    }

    fun createRelationInputWithRefinedLegalEntityBPs(seed: String, relationType: RelationType = RelationType.IsManagedBy): RelationDto {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntity(source)
        businessPartner.refineToLegalEntity(target)
        return relationInput
    }

    fun createLegalEntityRelationOutput(seed: String, relationType: RelationType = RelationType.IsManagedBy): Pair<RelationDto, BusinessPartnerRelations> {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntity(source)
        businessPartner.refineToLegalEntity(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun createAddressRelationOutput(seed: String, relationType: RelationType = RelationType.IsReplacedBy): Pair<RelationDto, BusinessPartnerRelations> {
        val source = businessPartner.upsertInput("$seed Source")
        val target = businessPartner.upsertInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        businessPartner.refineToLegalEntityOnSite(source)
        businessPartner.refineToAdditionalAddressOfSite(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun updateLegalEntityRelationOutput(original: RelationDto, updateSeed: String, relationType: RelationType): BusinessPartnerRelations {
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

    fun refineRelationToSuccess(input: RelationDto, seed: String = input.externalId): BusinessPartnerRelations {
        val refinementResult = orchestratorMockDataFactory.mockRefineRelation(seed).businessPartnerRelationsResult
        relationTaskCreationService.sendTasks()
        relationTaskResolutionService.checkResolveTasks()
        return refinementResult
    }
}
