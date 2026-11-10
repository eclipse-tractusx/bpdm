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

package org.eclipse.tractusx.bpdm.cleaning.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestratorV7Client
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepReservationEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskRelationsStepResultV7Request
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationV7Request
import org.springframework.stereotype.Service

@Service
class RelationCleaningServiceDummy(
    private val orchestrationApiClient: OrchestratorV7Client,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun relationPollForCleanAndSyncTasks() {
        processRelationPollingTasks(cleaningServiceConfigProperties.step)
    }

    private fun processRelationPollingTasks(step: TaskStepV7) {
        try {
            logger.debug { "Starting polling for relation cleaning tasks from Orchestrator... TaskStep ${step.name}" }

            do {
                val cleaningRelationRequest =
                    orchestrationApiClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationV7Request(amount = 10, step))

                val cleaningRelationTasks = cleaningRelationRequest.reservedTasks

                logger.debug { "${cleaningRelationTasks.size} relation tasks found for cleaning. Proceeding with cleaning..." }

                if (cleaningRelationTasks.isNotEmpty()) {
                    val cleaningRelationResults = cleaningRelationTasks.map { reservedTask ->
                        processRelationCleaningTask(reservedTask)
                    }

                    orchestrationApiClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultV7Request(step, cleaningRelationResults))
                    logger.debug { "Relation cleaning tasks processing completed for this iteration." }
                }
            } while (cleaningRelationRequest.reservedTasks.isNotEmpty())
        } catch (e: Exception) {
            logger.error(e) { "Error while processing relation cleaning task" }
        }
    }

    fun processRelationCleaningTask(reservedTask: TaskRelationsStepReservationEntryV7Dto): TaskRelationsStepResultEntryV7Dto {
        val cleanedBusinessPartnerRelation = reservedTask.businessPartnerRelations
        return TaskRelationsStepResultEntryV7Dto(reservedTask.taskId, cleanedBusinessPartnerRelation)
    }

}