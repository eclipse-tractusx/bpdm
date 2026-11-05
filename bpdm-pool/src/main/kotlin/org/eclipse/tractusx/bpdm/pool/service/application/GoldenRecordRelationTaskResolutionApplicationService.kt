/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskKind
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.service.TaskRelationsStepBuildDispatcherService
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordRelationTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Upserts the relations of the golden record relation tasks the Orchestrator holds for the Pool and reports each task's result back.
 */
@Service
class GoldenRecordRelationTaskResolutionApplicationService(
    private val taskGateway: GoldenRecordRelationTaskGateway,
    private val stepBuildDispatcherService: TaskRelationsStepBuildDispatcherService,
    private val trackingService: GoldenRecordTaskTrackingService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {

    private val logger = KotlinLogging.logger { }

    // Each task commits on its own and the remote calls stay outside, so entries commit through a template rather than @Transactional.
    private val entryTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordRelationTaskResolutionApplicationService::class.qualifiedName}.resolveTasks")
    }

    /**
     * Resolves every relation task the Orchestrator holds for the Pool, one reservation at a time, until none is left.
     */
    fun resolveTasks() {
        var totalTasksProcessed = 0
        do {
            val reservedTasks = taskGateway.reserveTasks(taskProperties.batchSize)
            if (reservedTasks.isNotEmpty()) resolveReservation(reservedTasks)
            totalTasksProcessed += reservedTasks.size
        } while (reservedTasks.isNotEmpty())

        logger.debug { "Processed $totalTasksProcessed golden record relation tasks" }
    }

    private fun resolveReservation(reservedTasks: List<TaskRelationsStepReservationEntryDto>) {
        try {
            taskGateway.resolveTasks(reservedTasks.map(::resolve))
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
            trackingService.trackUnreported(GoldenRecordTaskKind.Relations, reservedTasks.map { it.taskId })
        }
    }

    private fun resolve(task: TaskRelationsStepReservationEntryDto): TaskRelationsStepResultEntryDto =
        try {
            entryTransaction.execute { stepBuildDispatcherService.upsertBusinessPartnerRelations(task) }!!
        } catch (ex: BpdmValidationException) {
            toErrorResult(task, ex.message ?: "")
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            toErrorResult(task, "An unexpected error occurred during Pool update")
        }

    private fun toErrorResult(task: TaskRelationsStepReservationEntryDto, description: String) =
        TaskRelationsStepResultEntryDto(
            taskId = task.taskId,
            errors = listOf(TaskRelationsErrorDto(type = TaskRelationsErrorType.Unspecified, description = description)),
            businessPartnerRelations = task.businessPartnerRelations
        )
}
