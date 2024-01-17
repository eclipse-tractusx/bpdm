/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.RequestWithKey
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TaskStepFetchAndReserveService(
    private val orchestrationClient: OrchestrationApiClient,
    private val taskStepBuildService: TaskStepBuildService,
    private val requestValidationService: RequestValidationService,
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {
    private val logger = KotlinLogging.logger { }

    @Scheduled(cron = "\${bpdm.client.pool-orchestrator.golden-record-scheduler-cron-expr:-}", zone = "UTC")
    fun fetchAndReserve() {
        try {
            logger.info { "Starting polling for cleaning tasks from Orchestrator..." }
            val reservationRequest = TaskStepReservationRequest(step = TaskStep.PoolSync, amount = 10)
            val taskStepReservation = orchestrationClient.goldenRecordTasks.reserveTasksForStep(reservationRequest = reservationRequest)

            logger.info { "${taskStepReservation.reservedTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

            if (taskStepReservation.reservedTasks.isNotEmpty()) {
                val taskResults = upsertGoldenRecordIntoPool(taskStepReservation.reservedTasks)
                orchestrationClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step = TaskStep.PoolSync, results = taskResults))
            }
            logger.info { "Cleaning tasks processing completed for this iteration." }
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
        }

    }

    fun upsertGoldenRecordIntoPool(taskEntries: List<TaskStepReservationEntryDto>): List<TaskStepResultEntryDto> {

        val taskEntryBpnMapping = TaskEntryBpnMapping(taskEntries, bpnRequestIdentifierRepository)

        val invalidTaskResultsByTaskEntry = validateTasks(taskEntries, taskEntryBpnMapping)

        val taskResults = taskEntries.map {

            val invalidTaskResult = invalidTaskResultsByTaskEntry[it]
            invalidTaskResult ?: businessPartnerTaskResult(it, taskEntryBpnMapping)
        }
        taskEntryBpnMapping.writeCreatedMappingsToDb(bpnRequestIdentifierRepository)
        return taskResults
    }

    private fun businessPartnerTaskResult(taskStep: TaskStepReservationEntryDto, taskEntryBpnMapping: TaskEntryBpnMapping): TaskStepResultEntryDto {

        return try {
            taskStepBuildService.upsertBusinessPartner(taskStep, taskEntryBpnMapping)
        } catch (ex: BpdmValidationException) {
            TaskStepResultEntryDto(
                taskId = taskStep.taskId,
                errors = listOf(
                    TaskErrorDto(
                        type = TaskErrorType.Unspecified,
                        description = ex.message ?: ""
                    )
                )
            )
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            TaskStepResultEntryDto(
                taskId = taskStep.taskId,
                errors = listOf(
                    TaskErrorDto(
                        type = TaskErrorType.Unspecified,
                        description = "An unexpected error occurred during Pool update"
                    )
                )
            )
        }
    }

    private fun validateTasks( taskEntries: List<TaskStepReservationEntryDto>,  requestMappings: TaskEntryBpnMapping
    ): Map<TaskStepReservationEntryDto, TaskStepResultEntryDto?> {

        val validationErrorsByTaskEntry = requestValidationService.validateTasksFromOrchestrator(taskEntries, requestMappings)

        val taskResultsByTaskEntry = taskEntries
            .associateWith { taskEntry ->
                taskStepResultEntryDtoOnExistingError(taskEntry, validationErrorsByTaskEntry)
            }
            .filterValues { it != null }
        return taskResultsByTaskEntry
    }

    private fun taskStepResultEntryDtoOnExistingError(
        taskEntry: TaskStepReservationEntryDto,
        validationErrorsByTaskEntry: Map<RequestWithKey, Collection<ErrorInfo<ErrorCode>>>
    ): TaskStepResultEntryDto? {

        val errors = validationErrorsByTaskEntry[taskEntry]
        return if (errors != null) {
            taskStepResultEntryDto(taskEntry.taskId, errors)
        } else {
            null
        }
    }

    private fun taskStepResultEntryDto(taskId: String, errors: Collection<ErrorInfo<ErrorCode>>): TaskStepResultEntryDto {

        return TaskStepResultEntryDto(taskId = taskId, errors = errors.map { TaskErrorDto(type = TaskErrorType.Unspecified, description = it.message) })
    }

}