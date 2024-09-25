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

package org.eclipse.tractusx.bpdm.gate.service

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.SyncRecordRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.ResultState
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskResultStateSearchRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskResolutionBatchService(
    private val taskResolutionService: TaskResolutionChunkService,
    private val entityManager: EntityManager
){
    private val logger = KotlinLogging.logger { }

    fun resolveTasks(){
        logger.info { "Start batch process for resolving pending tasks..." }

        var totalSuccesses = 0
        var totalErrors = 0
        var totalUnresolved = 0
        do{

            val results = taskResolutionService.resolveTasks()

            totalSuccesses += results.resolvedAsSuccess
            totalErrors += results.resolvedAsError
            totalUnresolved += results.unresolved

            entityManager.clear()
        }while (results.hasNextPage)

        logger.debug { "Total Resolved $totalSuccesses tasks as successful, $totalErrors as errors and $totalUnresolved still unresolved" }
    }

    fun healthCheck(){
        logger.info { "Start process Task Health Check..." }

        var totalTaskMissing = 0
        var totalProcessed = 0
        var pageToQuery = 0
        do{
            val result = taskResolutionService.healthCheck(pageToQuery)
            totalProcessed += result.processed
            totalTaskMissing += result.missing
            pageToQuery++

            entityManager.clear()
        }while (result.hasNextPage)

        logger.info { "Finished process Task Health Check: Processed $totalProcessed tasks with unhealthy $totalTaskMissing tasks" }
    }
}

@Service
class TaskResolutionChunkService(
    private val sharingStateRepository: SharingStateRepository,
    private val sharingStateService: SharingStateService,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val businessPartnerService: BusinessPartnerService,
    private val orchestratorMappings: OrchestratorMappings,
    private val synchRecordService: SyncRecordService,
    private val syncRecordRepository: SyncRecordRepository
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun healthCheck(pageToQuery: Int): HealthCheckResult{
        val pendingSharingStatePage = sharingStateRepository.findBySharingStateType(SharingStateType.Pending, PageRequest.of(pageToQuery, taskProperties.healthCheck.batchSize))
        val pendingSharingStates = pendingSharingStatePage.content
        val pendingTaskIds = pendingSharingStates.map {  it.taskId.toString() }

        val resultStates = orchestrationApiClient.goldenRecordTasks.searchTaskResultStates(TaskResultStateSearchRequest(pendingTaskIds)).resultStates

        val sharingStatesMissingTask = pendingSharingStates.zip(resultStates)
            .filter { (_, resultState) -> resultState == null }
            .map { (sharingState, _) -> sharingState }

        sharingStatesMissingTask.forEach { sharingStateService.setError(it, BusinessPartnerSharingError.MissingTaskID, "Missing Task in Orchestrator") }

        return HealthCheckResult(pendingSharingStates.size, sharingStatesMissingTask.size, pendingSharingStatePage.hasNext())
    }


    @Transactional
    fun resolveTasks(): ResolutionStats {

        val syncRecord = synchRecordService.getOrCreateRecord(SyncTypeDb.ORCHESTRATOR_FINISHED_TASKS)

        val events = orchestrationApiClient.finishedTaskEvents.getEvents(syncRecord.fromTime, PaginationRequest(0, 1000))
        val finishedTaskIds = events.content.map { it.taskId }.toSet()

        val foundSharingStates = sharingStateRepository.findByTaskIdIn(finishedTaskIds)
        val pendingSharingStates = foundSharingStates.filter { it.sharingStateType == SharingStateType.Pending }

        val tasks = foundSharingStates.chunked(taskProperties.check.batchSize){ chunkedSharingStates ->
            orchestrationApiClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(chunkedSharingStates.map { TaskStateRequest.Entry(taskId = it.taskId.toString(), recordId = it.orchestratorRecordId.toString()) })).tasks
        }.flatten()

        val tasksById = tasks.associateBy { it.taskId }

        val inputs = businessPartnerRepository.findBySharingStateInAndStage(pendingSharingStates, StageType.Input)
        val inputsByExternalId = inputs.associateBy { it.sharingState.externalId }

        val mappingResults = pendingSharingStates.map { sharingState ->
            tryCreateUpsertRequest(sharingState, tasksById[sharingState.taskId], inputsByExternalId[sharingState.externalId])
        }

        val successes = mappingResults.filter { it.businessPartnerResult != null }
        val errors = mappingResults.filter { it.errorType != null }
        val unresolved = mappingResults.filter { it.businessPartnerResult == null && it.errorType == null }

        resolveAsUpserts(successes)
        resolveAsErrors(errors)

        events.content.lastOrNull()?.let { latestEvent ->
            syncRecord.fromTime = latestEvent.timestamp
            syncRecordRepository.save(syncRecord)
        }


        logger.debug { "Resolved ${successes.size} tasks as successful, ${errors.size} as errors and ${unresolved.size} still unresolved" }

        return ResolutionStats(successes.size, errors.size, unresolved.size, events.totalPages > 1)
    }

    private fun tryCreateUpsertRequest(sharingState: SharingStateDb, task: TaskClientStateDto?, input: BusinessPartnerDb?): RequestCreationResult {
        if (task == null) {
            return RequestCreationResult.error(sharingState, BusinessPartnerSharingError.MissingTaskID, "Missing Task in Orchestrator")
        }

        return when (task.processingState.resultState) {
            ResultState.Pending -> RequestCreationResult(sharingState, null, null, null)
            ResultState.Success -> createUpsertRequestForSuccessfulTask(sharingState, task, input)
            ResultState.Error -> RequestCreationResult.error(
                sharingState,
                BusinessPartnerSharingError.SharingProcessError,
                if (task.processingState.errors.isNotEmpty()) task.processingState.errors.joinToString(" // ") { it.description }.take(255) else null
            )
        }
    }

    private fun createUpsertRequestForSuccessfulTask(sharingState: SharingStateDb, task: TaskClientStateDto, input: BusinessPartnerDb?): RequestCreationResult {
        if (input == null) {
            return RequestCreationResult.error(
                sharingState,
                BusinessPartnerSharingError.SharingProcessError,
                "No input data found for the references business partner"
            )
        }

        val upsertRequest = try {
            orchestratorMappings.toOutputUpsertData(task.businessPartnerResult, input.roles.toList(), sharingState.tenantBpnl)
        } catch (ex: Throwable) {
            return RequestCreationResult.error(sharingState, BusinessPartnerSharingError.SharingProcessError, ex.message?.take(255))
        }

        return RequestCreationResult.success(sharingState, upsertRequest)
    }

    private fun resolveAsUpserts(requests: List<RequestCreationResult>) {
        requests.forEach { sharingStateService.setSuccess(it.sharingState) }
        businessPartnerService.upsertBusinessPartnersOutput(requests.map {
            BusinessPartnerService.OutputUpsertRequest(
                it.sharingState,
                it.businessPartnerResult!!
            )
        })
    }

    private fun resolveAsErrors(errors: List<RequestCreationResult>) {
        errors.forEach {
            sharingStateService.setError(it.sharingState, it.errorType!!, it.errorMessage)
        }
    }

    private data class RequestCreationResult(
        val sharingState: SharingStateDb,
        val businessPartnerResult: OutputUpsertData?,
        val errorType: BusinessPartnerSharingError?,
        val errorMessage: String?
    ) {
        companion object {
            fun error(sharingState: SharingStateDb, errorType: BusinessPartnerSharingError, errorMessage: String?) =
                RequestCreationResult(sharingState, null, errorType, errorMessage)

            fun success(sharingState: SharingStateDb, request: OutputUpsertData) =
                RequestCreationResult(sharingState, request, null, null)
        }
    }

    data class ResolutionStats(
        val resolvedAsSuccess: Int,
        val resolvedAsError: Int,
        val unresolved: Int,
        val hasNextPage: Boolean
    )

    data class HealthCheckResult(
        val processed: Int,
        val missing: Int,
        val hasNextPage: Boolean
    )

}