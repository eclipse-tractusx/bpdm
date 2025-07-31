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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

@Service
class TaskRelationsBatchResolutionService(
    private val taskRelationsResolutionService: TaskRelationsResolutionService,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val orchestrationClient: OrchestrationApiClient,
    private val goldenRecordTaskRepository: GoldenRecordTaskRepository,
    private val entityManager: EntityManager,
    ) {

    private val logger = KotlinLogging.logger { }

    @Scheduled(cron = "#{${GoldenRecordTaskConfigProperties.GET_CRON}}", zone = "UTC")
    fun processTasks(){
        logger.info { "Start Relations golden record task processing schedule..." }
        reserveAndResolve()
        resolveUnresolved()
        deleteResolved()
    }

    fun reserveAndResolve(){
        var totalTasksProcessed = 0
        do{
            val reservationRequest = TaskStepReservationRequest(step = goldenRecordTaskConfigProperties.step, amount = goldenRecordTaskConfigProperties.batchSize)
            val taskStepReservation = orchestrationClient.relationsGoldenRecordTasks.reserveTasksForStep(reservationRequest = reservationRequest)

            val successfullyResolved = try{
                taskRelationsResolutionService.resolveTasks(taskStepReservation)
                true
            } catch (ex: Throwable) {
                logger.error(ex) { "Error while processing cleaning task" }
                false
            }

            totalTasksProcessed += taskStepReservation.reservedTasks.size

            if(!successfullyResolved)
                goldenRecordTaskRepository.saveAll(taskStepReservation.reservedTasks.map { GoldenRecordTaskDb(it.taskId, true, Instant.now()) })

            entityManager.clear()
        }while (taskStepReservation.reservedTasks.isNotEmpty())

        logger.info { "Total of $totalTasksProcessed processed" }
    }

    fun resolveUnresolved(){
        val scheduleTime = Instant.now()

        val resultTemplate = TaskRelationsStepResultEntryDto("", BusinessPartnerRelationVerboseDto.empty, listOf(TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, "Unknown golden record process error during Pool communication")))

        var processedTasks = 0
        var resolvedTasks = 0
        do{
            val task = goldenRecordTaskRepository.findFirstByLastCheckedBefore(scheduleTime)?.let { task ->
                if(!task.isResolved){
                    try{
                        orchestrationClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(goldenRecordTaskConfigProperties.step, listOf(resultTemplate.copy(taskId = task.taskId))))
                        task.isResolved = true
                        resolvedTasks++
                    }catch (e: WebClientResponseException.BadRequest){
                        logger.warn { "Tracked task ${task.taskId} seems to be missing in Orchestrator. Marking it as resolved..." }
                        task.isResolved = true
                        resolvedTasks++
                    }catch (e: WebClientException){
                        logger.error { "Could not resolve task '${task.taskId}: ${e.message}'" }
                        task.isResolved = false
                    }
                }

                task.lastChecked = scheduleTime
                goldenRecordTaskRepository.save(task)
                entityManager.clear()

                processedTasks++
            }
        }while (task != null)

        logger.info { "Resolving tasks: Checked $processedTasks tasks and resolved $resolvedTasks tasks as errors" }

    }

    fun deleteResolved(){
        var tasksDeleted = 0
        do{
            val task = goldenRecordTaskRepository.findFirstByIsResolved(true)?.let { task ->
                goldenRecordTaskRepository.delete(task)
                entityManager.clear()
                tasksDeleted++
            }
        }while (task != null)

        logger.info { "Deleted $tasksDeleted resolved tasks" }
    }

}

@Service
class TaskRelationsResolutionService(
    private val orchestrationClient: OrchestrationApiClient,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val taskRelationsStepBuildService: TaskRelationsStepBuildService
) {
    private val logger = KotlinLogging.logger { }

    fun resolveTasks(taskStepReservation: TaskRelationsStepReservationResponse) {
        logger.info { "${taskStepReservation.reservedTasks.size} tasks found for cleaning. Proceeding with cleaning..." }

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
        logger.info { "Cleaning tasks processing completed for this iteration." }
    }

    fun upsertRelationsGoldenRecordIntoPool(taskEntries: List<TaskRelationsStepReservationEntryDto>) : List<TaskRelationsStepResultEntryDto> {
        val taskResults = taskEntries.map { businessPartnerRelationsTaskResult(it) }

        return taskResults
    }

    private fun businessPartnerRelationsTaskResult(taskStep: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        return try {
            taskRelationsStepBuildService.upsertBusinessPartnerRelations(taskStep)
        }catch (ex: BpdmValidationException) {
            TaskRelationsStepResultEntryDto(
                taskId = taskStep.taskId,
                errors = listOf(
                    TaskRelationsErrorDto(
                        type = TaskRelationsErrorType.Unspecified,
                        description = ex.message ?: ""
                    )
                ),
                businessPartnerRelations = BusinessPartnerRelationVerboseDto(
                    relationType = taskStep.businessPartnerRelations.relationType,
                    businessPartnerSourceBpnl = taskStep.businessPartnerRelations.businessPartnerSourceBpnl,
                    businessPartnerTargetBpnl = taskStep.businessPartnerRelations.businessPartnerTargetBpnl,
                    validFrom = taskStep.businessPartnerRelations.validFrom,
                    validTo = taskStep.businessPartnerRelations.validTo,
                    isActive = Instant.now() in taskStep.businessPartnerRelations.validFrom..taskStep.businessPartnerRelations.validTo
                )
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
                businessPartnerRelations = BusinessPartnerRelationVerboseDto(
                    relationType = taskStep.businessPartnerRelations.relationType,
                    businessPartnerSourceBpnl = taskStep.businessPartnerRelations.businessPartnerSourceBpnl,
                    businessPartnerTargetBpnl = taskStep.businessPartnerRelations.businessPartnerTargetBpnl,
                    validFrom = taskStep.businessPartnerRelations.validFrom,
                    validTo = taskStep.businessPartnerRelations.validTo,
                    isActive = Instant.now() in taskStep.businessPartnerRelations.validFrom..taskStep.businessPartnerRelations.validTo
                )
            )
        }
    }

}