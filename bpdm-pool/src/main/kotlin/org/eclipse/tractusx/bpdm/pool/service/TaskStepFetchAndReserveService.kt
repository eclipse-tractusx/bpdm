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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskReservationBatchService(
    private val taskStepFetchAndReserveService: TaskStepFetchAndReserveService
){
    private val logger = KotlinLogging.logger { }

    @Scheduled(cron = "#{${GoldenRecordTaskConfigProperties.GET_CRON}}", zone = "UTC")
    fun process(){
        logger.info { "Starting polling for cleaning tasks from Orchestrator..." }

        var totalTasksProcessed = 0
        do{
            val tasksProcessed = taskStepFetchAndReserveService.fetchAndReserve()
            totalTasksProcessed += tasksProcessed
        }while (tasksProcessed != 0)

        logger.info { "Total of $totalTasksProcessed processed" }
    }
}

@Service
class TaskStepFetchAndReserveService(
    private val orchestrationClient: OrchestrationApiClient,
    private val taskStepBuildService: TaskStepBuildService,
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun fetchAndReserve(): Int {
        try {
            logger.info { "Reserving next chunk of cleaning tasks from Orchestrator..." }
            val reservationRequest = TaskStepReservationRequest(step = TaskStep.PoolSync, amount = goldenRecordTaskConfigProperties.batchSize)
            val taskStepReservation = orchestrationClient.goldenRecordTasks.reserveTasksForStep(reservationRequest = reservationRequest)

            logger.info { "${taskStepReservation.reservedTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

            if (taskStepReservation.reservedTasks.isNotEmpty()) {
                val taskResults = upsertGoldenRecordIntoPool(taskStepReservation.reservedTasks)
                orchestrationClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step = TaskStep.PoolSync, results = taskResults))
            }
            logger.info { "Cleaning tasks processing completed for this iteration." }

            return taskStepReservation.reservedTasks.size
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
            return 0
        }
    }

    fun upsertGoldenRecordIntoPool(taskEntries: List<TaskStepReservationEntryDto>): List<TaskStepResultEntryDto> {

        val taskEntryBpnMapping = TaskEntryBpnMapping(taskEntries, bpnRequestIdentifierRepository)

        val taskResults = taskEntries.map { businessPartnerTaskResult(it, taskEntryBpnMapping) }
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
                ),
                businessPartner = taskStep.businessPartner
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
                ),
                businessPartner = taskStep.businessPartner
            )
        }
    }

}