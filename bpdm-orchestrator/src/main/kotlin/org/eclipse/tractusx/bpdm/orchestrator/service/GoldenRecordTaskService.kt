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

package org.eclipse.tractusx.bpdm.orchestrator.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.fetchBusinessPartnerData
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class GoldenRecordTaskService(
    private val goldenRecordTaskStateMachine: GoldenRecordTaskStateMachine,
    private val taskConfigProperties: TaskConfigProperties,
    private val responseMapper: ResponseMapper,
    private val taskRepository: GoldenRecordTaskRepository
) {

    private val logger = KotlinLogging.logger { }

    fun searchTaskResultStates(stateRequest: TaskResultStateSearchRequest): TaskResultStateSearchResponse{
        logger.debug { "Search for ${stateRequest.taskIds.size} task result states" }

        val uuidsToSearch = stateRequest.taskIds.map { toUUID(it) }.toSet()
        val tasksByUuid  = taskRepository.findByUuidIn(uuidsToSearch).associateBy { it.uuid }

        return TaskResultStateSearchResponse(uuidsToSearch
            .map { tasksByUuid[it]?.processingState?.resultState }
            .map { it?.let { responseMapper.toResultState(it) }
            })
    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        logger.debug { "Search for the state of golden record task: executing searchTaskStates() with parameters $stateRequest" }
        val requestsByTaskId = stateRequest.entries.associateBy { it.taskId }

        return stateRequest.entries.map { toUUID(it.taskId) }
            .let { uuids -> taskRepository.findByUuidIn(uuids.toSet()) }
            .also { tasks -> taskRepository.fetchBusinessPartnerData(tasks) }
            .filter { task -> requestsByTaskId[task.uuid.toString()]?.recordId == task.gateRecord.privateId.toString() }
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskStateResponse(tasks = it) }
    }

    @Transactional
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse {
        logger.debug { "Reservation of next golden record tasks: executing reserveTasksForStep() with parameters $reservationRequest" }
        val now = Instant.now()

        val foundTasks = taskRepository.findByStepAndStepState(reservationRequest.step, GoldenRecordTaskDb.StepState.Queued, Pageable.ofSize(reservationRequest.amount))
            .content.toSet()
            .also { taskRepository.fetchBusinessPartnerData(it) }
        val reservedTasks = foundTasks.map { goldenRecordTaskStateMachine.doReserve(it) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        logger.debug { "Reserved ${reservedTasks.size} golden record tasks for step ${reservationRequest.step}: ${reservedTasks.toLogIdentifiers()}" }

        return reservedTasks
            .map { task ->
                TaskStepReservationEntryDto(
                    task.uuid.toString(),
                    task.gateRecord.publicId.toString(),
                    responseMapper.toBusinessPartnerResult(task.businessPartner)
                )
            }
            .let { reservations -> TaskStepReservationResponse(reservations, pendingTimeout) }
    }

    @Transactional
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        logger.debug { "Step results for reserved golden record tasks: executing resolveStepResults() with parameters $resultRequest" }
        resultRequest.results.filter { it.errors.isEmpty() }.forEach { assertAdditionalSitesHaveSite(it.businessPartner) }

        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.toSet()).also { taskRepository.fetchBusinessPartnerData(it) }
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        val resolvedTasks = resultRequest.results
            .map { resultEntry -> Pair(foundTasksByUuid[resultEntry.taskId] ?: throw BpdmTaskNotFoundException(resultEntry.taskId), resultEntry) }
            .filterNot { (task, _) -> task.processingState.resultState == GoldenRecordTaskDb.ResultState.Aborted }
            .mapNotNull { (task, resultEntry) ->
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner

                when{
                    errors.isNotEmpty() -> goldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                    else ->  goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, resultBusinessPartner)
                }
            }

        logResolvedTasks(resolvedTasks, resultRequest.step)
    }

    private fun logResolvedTasks(resolvedTasks: List<GoldenRecordTaskDb>, step: TaskStep) {
        val tasksByResultState = resolvedTasks.groupBy { it.processingState.resultState }

        tasksByResultState[GoldenRecordTaskDb.ResultState.Pending]
            ?.groupBy { it.processingState.step }
            ?.forEach { (nextStep, tasks) ->
                logger.info { "Advanced ${tasks.size} golden record tasks from step $step to step $nextStep: ${tasks.toLogIdentifiers()}" }
            }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Success]
            ?.let { tasks -> logger.info { "Completed ${tasks.size} golden record tasks after step $step: ${tasks.toLogIdentifiers()}" } }
        tasksByResultState[GoldenRecordTaskDb.ResultState.Error]
            ?.let { tasks -> logger.info { "Failed ${tasks.size} golden record tasks in step $step: ${tasks.toLogIdentifiers()}" } }
    }

    @Transactional
    fun processPendingTimeouts(pageSize: Int): PaginationInfo {
        val timedOutTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStatePendingTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                goldenRecordTaskStateMachine.doResolveTaskToTimeout(task)
                timedOutTasks.add(task)
            }
        ).also {
            if (timedOutTasks.isNotEmpty())
                logger.info { "Timed out ${timedOutTasks.size} golden record tasks: ${timedOutTasks.toLogIdentifiers()}" }
        }
    }

    @Transactional
    fun processRetentionTimeouts(pageSize: Int): PaginationInfo {
        val deletedTasks = mutableListOf<GoldenRecordTaskDb>()

        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStateRetentionTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                taskRepository.delete(task)
                deletedTasks.add(task)
            }
        ).also {
            if (deletedTasks.isNotEmpty())
                logger.info { "Deleted ${deletedTasks.size} golden record tasks after their retention timeout: ${deletedTasks.toLogIdentifiers()}" }
        }
    }

    private fun batchProcessTasks(
        pageSize: Int,
        fetchPage: (Pageable) -> Page<GoldenRecordTaskDb>,
        processTask: (GoldenRecordTaskDb) -> Unit
    ): PaginationInfo {
        val pageable: Pageable = PageRequest.of(0, pageSize)
        val page = fetchPage(pageable)
        var hasProcessedTasks = false
        var processedTaskCount = 0

        page.forEach { task ->
            try {
                processTask(task)
                hasProcessedTasks = true
                processedTaskCount++ // Increment on successful processing
            } catch (err: RuntimeException) {
                logger.error(err) { "Error processing timeout for task ${task.uuid}" }
            }
        }

        return PaginationInfo(hasProcessedTasks, page.hasNext(), processedTaskCount)
    }

    private fun calculateTaskPendingTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskPendingTimeout)

    private fun calculateTaskRetentionTimeout(task: GoldenRecordTaskDb) =
        task.createdAt.instant.plus(taskConfigProperties.taskRetentionTimeout)

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmTaskNotFoundException(uuidString)
        }

    private fun assertAdditionalSitesHaveSite(businessPartner: BusinessPartner) {
        val hasSite = businessPartner.site?.bpnReference?.referenceValue != null || businessPartner.site?.siteName != null
        if (businessPartner.additionalSites.isNotEmpty() && !hasSite) {
            logger.warn { "Business partner has additional sites but no main site" }
        }
    }
}

private fun Collection<GoldenRecordTaskDb>.toLogIdentifiers() =
    map { it.uuid.toString() }.joinIdentifiersForLog()

data class PaginationInfo(
    val hasProcessedTasks: Boolean,
    val hasNextPage: Boolean,
    val processedTaskCount: Int
) {
    fun countProcessedTasks(): Int = processedTaskCount
}
