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

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class RelationTaskCreationService(
    private val taskConfigProperties: GoldenRecordTaskConfigProperties,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
    private val relationRepository: RelationRepository,
    private val relationStageRepository: RelationStageRepository,
    private val orchestratorClient: OrchestrationApiClient,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val relationSharingStateService: RelationSharingStateService
) {
    private val logger = KotlinLogging.logger { }

    fun sendTasks(): Int{
        logger.info { "Started scheduled task to create golden record tasks from business partner relations" }

        stageRelations()
        val totalSentCount = sendStagedRelations()

        logger.debug { "Total created $totalSentCount new golden record tasks from business partner relations" }

        return totalSentCount
    }

    private fun stageRelations(){
        var stagedCount = 0
        do{
            stagedCount = transactionTemplate.execute { stageRelationsForSending(taskConfigProperties.relationCreation.batchSize) } ?: 0

            entityManager.clear()
        }while (stagedCount > 0)
    }

    private fun sendStagedRelations(): Int{
        var totalSentCount = 0
        var sentCount = 0
        do{
            sentCount = transactionTemplate.execute { sendTaskBatch(taskConfigProperties.relationCreation.batchSize) } ?: 0
            totalSentCount += sentCount
            entityManager.clear()
        }while (sentCount > 0)

        return totalSentCount
    }

    fun stageRelationsForSending(batchSize: Int): Int{
        val toStagePage = relationRepository.findBySharingStateAndStaged(RelationSharingStateType.Ready, false, PageRequest.ofSize(batchSize))
        toStagePage.content.forEach(::stage)
        return toStagePage.content.size
    }

    fun sendTaskBatch(batchSize: Int): Int{
        val toSendPage = relationRepository.findBySharingStateAndStaged(RelationSharingStateType.Ready, true, PageRequest.ofSize(batchSize))
        val toSendRelations = toSendPage.content

        val toSendStages = relationStageRepository.findByRelationInAndStage(toSendRelations.toSet(), StageType.Input)
        val stagesByRelation = toSendStages.associateBy { it.relation.id }

        val sharingStates = toSendStages.map { it.source } + toSendStages.map { it.target }
        val outputs = businessPartnerRepository.findBySharingStateInAndStage(sharingStates.toSet(), StageType.Output)
        val outputsBySharingState = outputs.associateBy { it.sharingState }

        val taskCreateRequests = toSendRelations.map{ relation ->
            val sharingState = relation.sharingState ?: return@map null
            val relationStage = stagesByRelation[relation.id] ?: return@map null
            val sourceBpnL = outputsBySharingState[relationStage.source]?.bpnL ?: return@map null
            val targetBpnL = outputsBySharingState[relationStage.target]?.bpnL ?: return@map null
            val relationType = relationStage.relationType.toOrchestratorModel() ?: return@map null

            TaskCreateRelationsRequestEntry(sharingState.recordId, BusinessPartnerRelations(relationType, sourceBpnL, targetBpnL))
        }

        val createdTasks = taskCreateRequests.letNonNull { sendTasks(it) }

        toSendRelations.zip(createdTasks){ relation, createdTask ->
            if (createdTask != null){
                relationSharingStateService.setPending(relation, createdTask.taskId, createdTask.recordId)
            }else{
                unstage(relation)
            }
        }

        return createdTasks.filterNotNull().size
    }

    private fun sendTasks(taskCreateRequests: List<TaskCreateRelationsRequestEntry>): List<TaskClientRelationsStateDto?>{
        return try{
            if(taskCreateRequests.isEmpty())
                emptyList()
            else
                orchestratorClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsRequest(TaskMode.UpdateFromSharingMember, taskCreateRequests)).createdTasks
        }catch (_: Throwable){
            logger.error { "Error encountered when trying to create tasks at the orchestrator" }
            taskCreateRequests.map { null }
        }
    }

    private fun RelationType.toOrchestratorModel(): org.eclipse.tractusx.orchestrator.api.model.RelationType?{
        return when(this){
            RelationType.IsManagedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> org.eclipse.tractusx.orchestrator.api.model.RelationType.IsOwnedBy
        }
    }

    private fun stage(relation: RelationDb){
        relation.sharingState?.isStaged = true
        relationRepository.save(relation)
    }

    private fun unstage(relation: RelationDb){
        relation.sharingState?.isStaged = false
        relationRepository.save(relation)
    }


    private fun <INPUT, OUTPUT> List<INPUT?>.letNonNull(transform: (List<INPUT>) -> List<OUTPUT?>): List<OUTPUT?>{
        val result = transform(filterNotNull())
        var resultIndex = 0
        return map { element ->
            if(element == null) return@map null
            result[resultIndex++]
        }
    }
}