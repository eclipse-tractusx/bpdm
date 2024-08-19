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

package org.eclipse.tractusx.bpdm.orchestrator.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.orchestrator.config.TaskConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.entity.DbTimestamp
import org.eclipse.tractusx.bpdm.orchestrator.entity.GateRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GateRecordRepository
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
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
    private val taskRepository: GoldenRecordTaskRepository,
    private val gateRecordRepository: GateRecordRepository
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        logger.debug { "Creation of new golden record tasks: executing createTasks() with parameters $createRequest" }

        val gateRecords = getOrCreateGateRecords(createRequest.requests)
        gateRecords.forEach { abortOutdatedTasks(it) }

        return createRequest.requests.zip(gateRecords)
            .map { (request, record) -> goldenRecordTaskStateMachine.initTask(createRequest.mode, request.businessPartner, record) }
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskCreateResponse(createdTasks = it) }
    }

    fun searchTaskStates(stateRequest: TaskStateRequest): TaskStateResponse {
        logger.debug { "Search for the state of golden record task: executing searchTaskStates() with parameters $stateRequest" }

        return stateRequest.taskIds.map { toUUID(it) }
            .let { uuids -> taskRepository.findByUuidIn(uuids.toSet()) }
            .map { task -> responseMapper.toClientState(task, calculateTaskRetentionTimeout(task)) }
            .let { TaskStateResponse(tasks = it) }
    }

    @Transactional
    fun reserveTasksForStep(reservationRequest: TaskStepReservationRequest): TaskStepReservationResponse {
        logger.debug { "Reservation of next golden record tasks: executing reserveTasksForStep() with parameters $reservationRequest" }
        val now = Instant.now()

        val foundTasks = taskRepository.findByStepAndStepState(reservationRequest.step, GoldenRecordTaskDb.StepState.Queued, Pageable.ofSize(reservationRequest.amount)).content
        val reservedTasks = foundTasks.map { task -> goldenRecordTaskStateMachine.doReserve(task) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

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
        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.toSet())
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        resultRequest.results
            .map { resultEntry -> Pair(foundTasksByUuid[resultEntry.taskId] ?: throw BpdmTaskNotFoundException(resultEntry.taskId), resultEntry) }
            .filterNot { (task, _) -> task.processingState.resultState == GoldenRecordTaskDb.ResultState.Aborted }
            .forEach { (task, resultEntry) ->
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner

                when{
                    errors.isNotEmpty() -> goldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                    else ->  goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, resultBusinessPartner)
                }
            }
    }

    @Transactional
    fun processPendingTimeouts(pageSize: Int): PaginationInfo {
        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStatePendingTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                logger.info { "Setting timeout for task ${task.uuid} after reaching pending timeout" }
                goldenRecordTaskStateMachine.doResolveTaskToTimeout(task)
            }
        )
    }

    @Transactional
    fun processRetentionTimeouts(pageSize: Int): PaginationInfo {
        return batchProcessTasks(pageSize,
            fetchPage = { pageable -> taskRepository.findByProcessingStateRetentionTimeoutBefore(DbTimestamp.now(), pageable) },
            processTask = { task ->
                logger.info { "Removing task ${task.uuid} after reaching retention timeout" }
                taskRepository.delete(task)
            }
        )
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

    private fun getOrCreateGateRecords(requests: List<TaskCreateRequestEntry>): List<GateRecordDb> {
        val privateIds = requests.map { request -> request.recordId?.let { toUUID(it) } }
        val notNullPrivateIds = privateIds.filterNotNull()

        val foundRecords = gateRecordRepository.findByPrivateIdIn(notNullPrivateIds.toSet())
        val foundRecordsByPrivateId = foundRecords.associateBy { it.privateId }
        val requestedNotFoundRecords = notNullPrivateIds.minus(foundRecordsByPrivateId.keys)

        if (requestedNotFoundRecords.isNotEmpty())
            throw BpdmRecordNotFoundException(requestedNotFoundRecords)

        return privateIds.map { privateId ->
            val gateRecord = privateId?.let { foundRecordsByPrivateId[it] } ?: GateRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID())
            gateRecordRepository.save(gateRecord)
        }
    }

    private fun abortOutdatedTasks(record: GateRecordDb){
        return taskRepository.findTasksByGateRecordAndProcessingStateResultState(record, GoldenRecordTaskDb.ResultState.Pending)
            .forEach { task -> goldenRecordTaskStateMachine.doAbortTask(task) }
    }
}

data class PaginationInfo(
    val hasProcessedTasks: Boolean,
    val hasNextPage: Boolean,
    val processedTaskCount: Int
) {
    fun countProcessedTasks(): Int = processedTaskCount
}
