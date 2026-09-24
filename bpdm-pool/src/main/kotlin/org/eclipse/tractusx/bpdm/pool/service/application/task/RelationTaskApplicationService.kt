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

package org.eclipse.tractusx.bpdm.pool.service.application.task

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.service.TaskRelationsStepBuildDispatcherService
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service

/**
 * Answers a batch of reserved golden record relation tasks with the relations they asked for.
 *
 * Each entry is written in its own transaction and answered on its own, so one rejected relation neither rolls back
 * nor hides the relations beside it.
 */
@Service
class RelationTaskApplicationService(
    private val successionUpsertApplicationService: SuccessionUpsertApplicationService,
    private val ownershipUpsertApplicationService: OwnershipUpsertApplicationService,
    private val dataManagementUpsertApplicationService: DataManagementUpsertApplicationService,
    private val taskRelationsStepBuildDispatcherService: TaskRelationsStepBuildDispatcherService
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Writes what each task asks for and reports the resulting relations, or that task's reason for not being written.
     */
    fun upsert(taskEntries: List<TaskRelationsStepReservationEntryDto>): List<TaskRelationsStepResultEntryDto> =
        taskEntries.map { upsert(it) }

    private fun upsert(taskEntry: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto =
        try {
            // Alternative headquarter designations are not yet parsed and written through the layered path; they still
            // reject by throwing, and move over in the task that owns them.
            when (taskEntry.businessPartnerRelations.relationType) {
                RelationType.IsReplacedBy -> successionUpsertApplicationService.upsert(taskEntry)
                RelationType.IsOwnedBy -> ownershipUpsertApplicationService.upsert(taskEntry)
                RelationType.IsManagedBy -> dataManagementUpsertApplicationService.upsert(taskEntry)
                RelationType.IsAlternativeHeadquarterFor -> taskRelationsStepBuildDispatcherService.upsertBusinessPartnerRelations(taskEntry)
            }
        } catch (ex: BpdmValidationException) {
            toErrorReply(taskEntry, ex.message ?: "")
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record relation task processing" }
            toErrorReply(taskEntry, "An unexpected error occurred during Pool update")
        }

    private fun toErrorReply(taskEntry: TaskRelationsStepReservationEntryDto, description: String): TaskRelationsStepResultEntryDto =
        TaskRelationsStepResultEntryDto(
            taskId = taskEntry.taskId,
            businessPartnerRelations = taskEntry.businessPartnerRelations,
            errors = listOf(TaskRelationsErrorDto(TaskRelationsErrorType.Unspecified, description))
        )
}
