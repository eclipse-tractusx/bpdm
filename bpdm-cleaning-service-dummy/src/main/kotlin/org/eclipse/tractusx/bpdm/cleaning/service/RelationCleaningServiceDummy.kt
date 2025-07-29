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

package org.eclipse.tractusx.bpdm.cleaning.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.cleaning.config.CleaningServiceConfigProperties
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationCleaningServiceDummy(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val cleaningServiceConfigProperties: CleaningServiceConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun relationPollForCleanAndSyncTasks() {
        processRelationPollingTasks(cleaningServiceConfigProperties.step)
    }

    private fun processRelationPollingTasks(step: TaskStep) {
        try {
            logger.info { "Starting polling for relation cleaning tasks from Orchestrator... TaskStep ${step.name}" }

            do {
                val cleaningRelationRequest =
                    orchestrationApiClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(amount = 10, step))

                val cleaningRelationTasks = cleaningRelationRequest.reservedTasks

                logger.info { "${cleaningRelationTasks.size} relation tasks found for cleaning. Proceeding with cleaning..." }

                if (cleaningRelationTasks.isNotEmpty()) {
                    val cleaningRelationResults = cleaningRelationTasks.map { reservedTask ->
                        processRelationCleaningTask(reservedTask)
                    }

                    orchestrationApiClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(step, cleaningRelationResults))
                    logger.info { "Relation cleaning tasks processing completed for this iteration." }
                }
            } while (cleaningRelationRequest.reservedTasks.isNotEmpty())
        } catch (e: Exception) {
            logger.error(e) { "Error while processing relation cleaning task" }
        }
    }

    fun processRelationCleaningTask(reservedTask: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto {
        return TaskRelationsStepResultEntryDto(
            reservedTask.taskId, BusinessPartnerRelationVerboseDto(
                relationType = reservedTask.businessPartnerRelations.relationType,
                businessPartnerSourceBpnl = reservedTask.businessPartnerRelations.businessPartnerSourceBpnl,
                businessPartnerTargetBpnl = reservedTask.businessPartnerRelations.businessPartnerTargetBpnl,
                validFrom = reservedTask.businessPartnerRelations.validFrom,
                validTo = reservedTask.businessPartnerRelations.validTo,
                isActive = Instant.now() in reservedTask.businessPartnerRelations.validFrom..reservedTask.businessPartnerRelations.validTo
            )
        )
    }

}