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
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class RelationTaskResolutionService(
    private val syncRecordService: SyncRecordService,
    private val orchestratorClient: OrchestrationApiClient,
    private val relationRepository: RelationRepository,
    private val taskConfigProperties: GoldenRecordTaskConfigProperties,
    private val relationService: IRelationService,
    private val sharingStateService: RelationSharingStateService,
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate
) {

    private val logger = KotlinLogging.logger { }

    fun checkResolveTasks(){
        logger.info { "Start batch process for resolving pending relation tasks..." }

        var totalSuccesses = 0
        var totalErrors = 0
        var totalUnresolved = 0
        var hasNextPage = false
        do{
            val results = transactionTemplate.execute {
                checkResolveTasks(taskConfigProperties.relationCheck.batchSize)
            } ?: ResolutionStats(0, 0, 0, false)

            entityManager.clear()

            totalSuccesses += results.resolvedAsSuccess
            totalErrors += results.resolvedAsError
            totalUnresolved += results.unresolved
            hasNextPage = results.hasNextPage
        }while (hasNextPage)

        logger.debug { "Total Resolved $totalSuccesses tasks as successful, $totalErrors as errors and $totalUnresolved still unresolved" }
    }

    fun checkResolveTasks(batchSize: Int): ResolutionStats{
        val syncRecord = syncRecordService.getOrCreateRecord(SyncTypeDb.ORCHESTRATOR_FINISHED_RELATION_TASKS)

        val events = orchestratorClient.relationsFinishedTaskEvents.getRelationsEvents(syncRecord.fromTime, PaginationRequest(0, 1000))
        val finishedTaskIds = events.content.map { it.taskId }.toSet()

        val foundRelations = relationRepository.findBySharingStateTaskIdIn(finishedTaskIds)
        val pendingRelations = foundRelations.filter { it.sharingState?.sharingStateType == RelationSharingStateType.Pending }
        val pendingRelationsById = pendingRelations.associateBy { it.sharingState!!.taskId!! }

        val tasks = searchOrchestratorTasks(pendingRelations, batchSize)
        val successfulTasks = tasks.filter { it.processingState.resultState == ResultState.Success }
        val errorTasks = tasks.filter { it.processingState.resultState == ResultState.Error }

        resolveAsSuccesses(successfulTasks, pendingRelationsById)
        resolveAsErrors(errorTasks, pendingRelationsById)

        syncRecordService.updateRecord(syncRecord,  events.content.lastOrNull()?.timestamp)

        val unresolvedSize = tasks.size - successfulTasks.size - errorTasks.size
        return ResolutionStats(successfulTasks.size, errorTasks.size, unresolvedSize, events.totalPages > 1)
    }

    private fun resolveAsSuccesses(tasks: List<TaskClientRelationsStateDto>, pendingRelationsById: Map<String, RelationDb>){
        val outputRequests = tasks.mapNotNull { task ->
            val relation = pendingRelationsById[task.taskId] ?: return@mapNotNull null
            val outputResult = task.businessPartnerRelationsResult
            IRelationService.RelationUpsertRequest(
                relation = relation,
                relationType = outputResult.relationType.toGateModel(),
                businessPartnerSourceExternalId = outputResult.businessPartnerSourceBpn,
                businessPartnerTargetExternalId = outputResult.businessPartnerTargetBpn,
                validityPeriods = outputResult.validityPeriods.map { it.toGateModel() },
                reasonCode = outputResult.reasonCode
            )
        }
        relationService.upsertOutputRelations(outputRequests)
    }

    private fun resolveAsErrors(tasks: List<TaskClientRelationsStateDto>, pendingRelationsById: Map<String, RelationDb>){
        tasks.forEach { task ->
            val relation = pendingRelationsById[task.taskId] ?: return@forEach
            val errorCode = task.processingState.errors.firstOrNull()?.type ?: TaskRelationsErrorType.Unspecified
            val errorMessage = task.processingState.errors.joinToString { it.description }.take(250)
            sharingStateService.setError(relation, errorCode.toGateModel(), errorMessage)
        }
    }

    private fun searchOrchestratorTasks(relations: List<RelationDb>, batchSize: Int): List<TaskClientRelationsStateDto>{
        return relations.chunked(batchSize){ chunkedRelations ->
            val requests = chunkedRelations.map { TaskStateRequest.Entry(it.sharingState!!.taskId!!, it.sharingState!!.recordId!!) }
            orchestratorClient.relationsGoldenRecordTasks.searchTaskStates(TaskStateRequest(requests)).tasks
        }.flatten()
    }

    private fun RelationType.toGateModel(): SharableRelationType{
        return when(this){
            RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> SharableRelationType.IsManagedBy
            RelationType.IsOwnedBy -> SharableRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> SharableRelationType.IsReplacedBy
        }
    }

    private fun TaskRelationsErrorType.toGateModel(): RelationSharingStateErrorCode{
        return when(this){
            TaskRelationsErrorType.Timeout -> RelationSharingStateErrorCode.SharingTimeout
            TaskRelationsErrorType.Unspecified -> RelationSharingStateErrorCode.SharingProcessError
        }
    }

    private fun RelationValidityPeriod.toGateModel(): RelationValidityPeriodDto {
        return RelationValidityPeriodDto(validFrom = this.validFrom, validTo = this.validTo)
    }


    data class ResolutionStats(
        val resolvedAsSuccess: Int,
        val resolvedAsError: Int,
        val unresolved: Int,
        val hasNextPage: Boolean
    )
}