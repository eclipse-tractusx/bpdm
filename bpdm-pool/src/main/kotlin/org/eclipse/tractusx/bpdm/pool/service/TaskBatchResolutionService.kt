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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.DbTimestamp
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.repository.GoldenRecordTaskRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

@Service
class TaskBatchResolutionService(
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val orchestrationClient: OrchestrationApiClient,
    private val goldenRecordTaskRepository: GoldenRecordTaskRepository,
    private val entityManager: EntityManager,

    ){
    private val logger = KotlinLogging.logger { }

    fun resolveUnresolved(){
        val scheduleTime = Instant.now()

        val resultTemplate = TaskStepResultEntryDto("", BusinessPartner.empty, listOf(TaskErrorDto(TaskErrorType.Unspecified, "Unknown golden record process error during Pool communication")))

        var processedTasks = 0
        var resolvedTasks = 0
        do{
            val task = goldenRecordTaskRepository.findFirstByLastCheckedBefore(scheduleTime)?.let { task ->
                if(!task.isResolved){
                    try{
                        orchestrationClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(goldenRecordTaskConfigProperties.step, listOf(resultTemplate.copy(taskId = task.taskId))))
                        task.isResolved = true
                        resolvedTasks++
                    }catch (_: WebClientResponseException.BadRequest){
                        logger.warn { "Tracked task ${task.taskId} seems to be missing in Orchestrator. Marking it as resolved..." }
                        task.isResolved = true
                        resolvedTasks++
                    }catch (e: WebClientException){
                        logger.error { "Could not resolve task '${task.taskId}: ${e.message}'" }
                        task.isResolved = false
                    }
                }

                task.lastChecked = scheduleTime
                task.updatedAt = DbTimestamp.now()
                goldenRecordTaskRepository.save(task)
                entityManager.clear()

                processedTasks++
            }
        }while (task != null)

        if (resolvedTasks > 0)
            logger.info { "Resolved $resolvedTasks of $processedTasks checked tasks as errors" }
        else
            logger.debug { "Checked $processedTasks tasks, none needed resolving as errors" }
    }

    fun deleteResolved(){
        // Each query re-asks what is left to delete, so a query has to see the preceding delete: keep this lazy.
        val tasksDeleted = generateSequence { goldenRecordTaskRepository.findFirstByIsResolved(true) }
            .onEach { task ->
                goldenRecordTaskRepository.delete(task)
                entityManager.clear()
            }
            .count()

        if (tasksDeleted > 0)
            logger.info { "Deleted $tasksDeleted resolved tasks" }
        else
            logger.debug { "No resolved tasks to delete" }
    }
}
