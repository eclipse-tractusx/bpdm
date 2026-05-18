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
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskCreationService
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskResolutionService
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.withRelationType
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.eclipse.tractusx.orchestrator.api.model.*

class GateTestDataClientV7(
    private val gateClient: GateClient,
    private val testDataFactory: TestDataFactoryGateV7,
    private val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    private val taskCreationBatchService: TaskCreationBatchService,
    private val taskResolutionBatchService: TaskResolutionBatchService,
    private val relationTaskResolutionService: RelationTaskResolutionService,
    private val relationTaskCreationService: RelationTaskCreationService,
    private val poolMockDataFactory: PoolMockDataFactory,
    private val tenantBpnL: String,
) {

    fun upsertBusinessPartnerInput(seed: String): BusinessPartnerInputDto {
        val request = testDataFactory.businessPartner.input.request.fromSeed(seed)
        return upsertBusinessPartnerInput(request)
    }

    fun upsertBusinessPartnerInput(request: BusinessPartnerInputRequest): BusinessPartnerInputDto {
        return gateClient.businessParters.upsertBusinessPartnersInput(listOf(request)).body!!.single()
    }

    fun upsertRelationInput(entry: RelationPutEntry, createIfNotExist: Boolean = true): RelationDto {
        return gateClient.relation.put(createIfNotExist, RelationPutRequest(listOf(entry))).upsertedRelations.single()
    }

    fun upsertRelationInput(externalId: String, source: BusinessPartnerInputDto, target: BusinessPartnerInputDto,  seed: String = externalId): RelationDto {
        val request = testDataFactory.relation.input.request.fromSeed(seed).copy(
            businessPartnerSourceExternalId = source.externalId,
            businessPartnerTargetExternalId = target.externalId
        )
        return gateClient.relation.put(true, RelationPutRequest(listOf(request))).upsertedRelations.single()
    }

    fun upsertRelationInputWithBusinessPartners(entry: RelationPutEntry, createIfNotExist: Boolean = true): RelationDto {
        upsertBusinessPartnerInput(entry.businessPartnerSourceExternalId)
        upsertBusinessPartnerInput(entry.businessPartnerTargetExternalId)
        return upsertRelationInput(entry, createIfNotExist)
    }

    fun upsertRelationInputWithBusinessPartners(seed: String, relationType: RelationType): RelationDto {
        return upsertRelationInputWithBusinessPartners(
            testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        )
    }

    fun setStateToReady(externalId: String) {
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
    }

    fun setStateToPending(externalId: String, seed: String = externalId): TaskClientStateDto {
        setStateToReady(externalId)
        val createdTask = orchestratorMockDataFactory.mockCreateTask(seed)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        return createdTask
    }

    fun createRelationWithOutputs(
        seed: String,
        relationType: RelationType,
        sourceRefine: (BusinessPartnerInputDto) -> Unit,
        targetRefine: (BusinessPartnerInputDto) -> Unit
    ): RelationDto {
        val source = upsertBusinessPartnerInput("$seed Source")
        val target = upsertBusinessPartnerInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).copy(relationType = relationType)
        val relation = upsertRelationInput(request)
        sourceRefine(source)
        targetRefine(target)
        return relation
    }

    fun createRelationInputWithRefinedLegalEntityBPs(seed: String, relationType: RelationType = RelationType.IsManagedBy): RelationDto {
        val source = upsertBusinessPartnerInput("$seed Source")
        val target = upsertBusinessPartnerInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        refineToLegalEntity(source)
        refineToLegalEntity(target)
        return relationInput
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

    fun setStateToSuccess(externalId: String, seed: String = externalId): TaskClientStateDto {
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(seed)
        val mockedRefinedTask = orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            null,
            emptyList()
        )
        setStateToReady(externalId)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()
        return mockedRefinedTask
    }

    fun setStateToError(externalId: String, seed: String = externalId, errorType: TaskErrorType): TaskClientStateDto {
        val errorTask = orchestratorMockDataFactory.mockSharingError(seed, errorType)
        setStateToReady(externalId)
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()
        return errorTask
    }

    fun createBusinessPartnerOutput(seed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertBusinessPartnerInput(seed)
        return refineToSuccess(upsertedInput)
    }

    fun updateBusinessPartnerOutput(output: BusinessPartnerOutputDto, newSeed: String): BusinessPartnerOutputDto {
        val upsertedInput = upsertBusinessPartnerInput(
            testDataFactory.businessPartner.input.request.fromSeed(newSeed).copy(externalId = output.externalId)
        )
        return refineToSuccess(upsertedInput)
    }

    fun refineToSuccess(input: BusinessPartnerInputDto, seed: String = input.externalId): BusinessPartnerOutputDto{
        prepareLegalEntityRefinement(input, seed)
        return shareBusinessPartnerAndResolve(input.externalId)
    }

    fun refineToLegalEntity(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto{
        val poolMockResult = prepareLegalEntityRefinement(input)
        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToLegalEntityOnSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
        val poolMockResult = poolMockDataFactory.mockLegalAndSiteMainAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToLegalEntityOnSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.site.site,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.SiteWithLegalEntityParent{
        val poolMockResult = poolMockDataFactory.mockSiteAndMainAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.site,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun refineToAdditionalAddressOfSite(input: BusinessPartnerInputDto, seed: String = input.externalId): PoolMockDataFactory.AdditionalAddressOfSiteResult{
        val poolMockResult = poolMockDataFactory.mockAdditionalAddressOfSiteSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToAdditionalAddressOfSite(
            seed,
            poolMockResult.legalEntityParent,
            poolMockResult.siteParent,
            poolMockResult.additionalAddress,
            owningCompany,
            input.nameParts
        )

        shareBusinessPartnerAndResolve(input.externalId)

        return poolMockResult
    }

    fun createLegalEntityRelationOutput(seed: String, relationType: RelationType = RelationType.IsManagedBy): Pair<RelationDto, BusinessPartnerRelations> {
        val source = upsertBusinessPartnerInput("$seed Source")
        val target = upsertBusinessPartnerInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        refineToLegalEntity(source)
        refineToLegalEntity(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun createAddressRelationOutput(seed: String, relationType: RelationType = RelationType.IsReplacedBy): Pair<RelationDto, BusinessPartnerRelations> {
        val source = upsertBusinessPartnerInput("$seed Source")
        val target = upsertBusinessPartnerInput("$seed Target")
        val request = testDataFactory.relation.input.request.fromSeed(seed).withRelationType(relationType)
        val relationInput = upsertRelationInput(request)
        refineToLegalEntityOnSite(source)
        refineToAdditionalAddressOfSite(target)
        val goldenRecord = refineRelationToSuccess(relationInput)
        return Pair(relationInput, goldenRecord)
    }

    fun updateLegalEntityRelationOutput(original: RelationDto, updateSeed: String, relationType: RelationType): BusinessPartnerRelations {
        val newSource = upsertBusinessPartnerInput("$updateSeed Source")
        val newTarget = upsertBusinessPartnerInput("$updateSeed Target")
        val updatedRequest = testDataFactory.relation.input.request.fromSeed(updateSeed).copy(
            externalId = original.externalId,
            relationType = relationType
        )
        val updatedRelationInput = upsertRelationInput(updatedRequest)
        refineToLegalEntity(newSource)
        refineToLegalEntity(newTarget)
        return refineRelationToSuccess(updatedRelationInput, updateSeed)
    }

    fun refineRelationToSuccess(input: RelationDto, seed: String = input.externalId): BusinessPartnerRelations{
        val refinementResult = orchestratorMockDataFactory.mockRefineRelation(seed).businessPartnerRelationsResult
        relationTaskCreationService.sendTasks()
        relationTaskResolutionService.checkResolveTasks()

        return refinementResult
    }


    private fun prepareLegalEntityRefinement(input: BusinessPartnerInputDto, seed: String = input.externalId): LegalEntityWithLegalAddressVerboseDto{
        val poolMockResult = poolMockDataFactory.mockLegalEntityAndLegalAddressSearchResult(seed)

        val owningCompany = if(input.isOwnCompanyData) tenantBpnL else null
        orchestratorMockDataFactory.mockRefineToLegalEntity(seed, poolMockResult, owningCompany, input.nameParts)

        return poolMockResult
    }

    private fun shareBusinessPartnerAndResolve(externalId: String): BusinessPartnerOutputDto{
        gateClient.sharingState.postSharingStateReady(PostSharingStateReadyRequest(listOf(externalId)))
        taskCreationBatchService.createTasksForReadyBusinessPartners()
        taskResolutionBatchService.resolveTasks()

        return gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId)).content.single()
    }
}