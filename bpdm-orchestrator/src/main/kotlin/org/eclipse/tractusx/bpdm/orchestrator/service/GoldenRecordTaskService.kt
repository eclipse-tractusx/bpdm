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
import org.eclipse.tractusx.bpdm.orchestrator.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmTaskNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
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

    @Transactional
    fun createTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        logger.debug { "Creation of new golden record tasks: executing createTasks() with parameters $createRequest" }

        return createRequest.businessPartners
            .map { businessPartnerData -> goldenRecordTaskStateMachine.initTask(createRequest.mode, businessPartnerData) }
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

        val foundTasks = taskRepository.findByStepAndStepState(reservationRequest.step, StepState.Queued, Pageable.ofSize(reservationRequest.amount)).content
        val reservedTasks = foundTasks.map { task -> goldenRecordTaskStateMachine.doReserve(task) }
        val pendingTimeout = reservedTasks.minOfOrNull { calculateTaskPendingTimeout(it) } ?: now

        return reservedTasks
            .map { task -> TaskStepReservationEntryDto(task.uuid.toString(), responseMapper.toBusinessPartnerResult(task.businessPartner)) }
            .let { reservations -> TaskStepReservationResponse(reservations, pendingTimeout) }
    }

    @Transactional
    fun resolveStepResults(resultRequest: TaskStepResultRequest) {
        logger.debug { "Step results for reserved golden record tasks: executing resolveStepResults() with parameters $resultRequest" }
        val uuids = resultRequest.results.map { toUUID(it.taskId) }
        val foundTasks = taskRepository.findByUuidIn(uuids.toSet())
        val foundTasksByUuid = foundTasks.associateBy { it.uuid.toString() }

        resultRequest.results
            .forEach { resultEntry ->
                val task = foundTasksByUuid[resultEntry.taskId]
                    ?: throw BpdmTaskNotFoundException(resultEntry.taskId)
                val step = resultRequest.step
                val errors = resultEntry.errors
                val resultBusinessPartner = resultEntry.businessPartner

                if (errors.isNotEmpty()) {
                    goldenRecordTaskStateMachine.doResolveTaskToError(task, step, errors)
                } else {
                    goldenRecordTaskStateMachine.resolveTaskStepToSuccess(task, step, resultBusinessPartner)
                }
            }
    }

    @Scheduled(cron = "\${bpdm.task.timeoutCheckCron}")
    @Transactional
    fun checkForTimeouts() {
        try {
            logger.debug { "Checking for timeouts" }
            checkForPendingTimeouts()
            checkForRetentionTimeouts()
        } catch (err: RuntimeException) {
            logger.error(err) { "Error checking for timeouts" }
        }
    }

    private fun checkForPendingTimeouts() {
        taskRepository.findByProcessingStatePendingTimeoutBefore(DbTimestamp.now())
            .forEach {
                try {
                    logger.info { "Setting timeout for task ${it.uuid} after reaching pending timeout" }
                    goldenRecordTaskStateMachine.doResolveTaskToTimeout(it)
                } catch (err: RuntimeException) {
                    logger.error(err) { "Error handling pending timeout for task ${it.uuid}" }
                }
            }
    }

    private fun checkForRetentionTimeouts() {
        taskRepository.findByProcessingStateRetentionTimeoutBefore(DbTimestamp.now())
            .forEach {
                try {
                    logger.info { "Removing task ${it.uuid} after reaching retention timeout" }
                    taskRepository.delete(it)
                } catch (err: RuntimeException) {
                    logger.error(err) { "Error handling retention timeout for task ${it.uuid}" }
                }
            }
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
}
