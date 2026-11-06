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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskKind
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service

@Service
class TaskRelationsBatchResolutionService(
    private val taskRelationsResolutionService: TaskRelationsResolutionService,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val orchestrationClient: OrchestrationApiClient,
    private val trackingService: GoldenRecordTaskTrackingService,
    ) {

    private val logger = KotlinLogging.logger { }

    fun reserveAndResolve(){
        var totalTasksProcessed = 0
        do{
            val reservationRequest = TaskStepReservationRequest(step = goldenRecordTaskConfigProperties.step, amount = goldenRecordTaskConfigProperties.batchSize)
            val taskStepReservation = orchestrationClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest = reservationRequest)

            try{
                taskRelationsResolutionService.resolveTasks(taskStepReservation)
            } catch (ex: Throwable) {
                logger.error(ex) { "Error while processing cleaning task" }
                trackingService.trackUnreported(GoldenRecordTaskKind.Relations, taskStepReservation.reservedTasks.map { it.taskId })
            }

            totalTasksProcessed += taskStepReservation.reservedTasks.size
        }while (taskStepReservation.reservedTasks.isNotEmpty())

        logger.debug { "Processed $totalTasksProcessed golden record relation tasks" }
    }
}

@Service
class TaskRelationsResolutionService(
    private val orchestrationClient: OrchestrationApiClient,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val taskRelationsStepBuildDispatcherService: TaskRelationsStepBuildDispatcherService
) {
    private val logger = KotlinLogging.logger { }

    fun resolveTasks(taskStepReservation: TaskRelationsStepReservationResponse) {
        logger.debug { "${taskStepReservation.reservedTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

        if (taskStepReservation.reservedTasks.isNotEmpty()) {
            val taskResults = upsertRelationsGoldenRecordIntoPool(taskStepReservation.reservedTasks)

            //Limit the length of errors so for the Orchestrator to not reject it
            val resultsWithSafeErrors = taskResults.map { result ->
                result.copy(errors = result.errors.map { error ->
                    error.copy(description = error.description.take(250))
                })
            }
            orchestrationClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(step = goldenRecordTaskConfigProperties.step, results = resultsWithSafeErrors))
        }
        logger.debug { "Cleaning tasks processing completed for this iteration." }
    }

    fun upsertRelationsGoldenRecordIntoPool(taskEntries: List<TaskRelationsStepReservationEntryDto>) : List<TaskRelationsStepResultEntryDto> {
        val taskResults = taskEntries.map { businessPartnerRelationsTaskResult(it) }

        return taskResults
    }

    private fun businessPartnerRelationsTaskResult(taskStep: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        return try {
            taskRelationsStepBuildDispatcherService.upsertBusinessPartnerRelations(taskStep)
        }catch (ex: BpdmValidationException) {
            TaskRelationsStepResultEntryDto(
                taskId = taskStep.taskId,
                errors = listOf(
                    TaskRelationsErrorDto(
                        type = TaskRelationsErrorType.Unspecified,
                        description = ex.message ?: ""
                    )
                ),
                businessPartnerRelations = taskStep.businessPartnerRelations
            )
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            TaskRelationsStepResultEntryDto(
                taskId = taskStep.taskId,
                errors = listOf(
                    TaskRelationsErrorDto(
                        type = TaskRelationsErrorType.Unspecified,
                        description = "An unexpected error occurred during Pool update"
                    )
                ),
                businessPartnerRelations = taskStep.businessPartnerRelations
            )
        }
    }

}