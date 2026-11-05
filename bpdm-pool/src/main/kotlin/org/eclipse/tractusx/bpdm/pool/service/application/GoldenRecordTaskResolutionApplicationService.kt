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
import org.eclipse.tractusx.bpdm.pool.exception.BpdmMultiValidationException
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.service.TaskStepBuildService
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorDto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Upserts the golden records of the tasks the Orchestrator holds for the Pool and reports each task's result back.
 */
@Service
class GoldenRecordTaskResolutionApplicationService(
    private val taskGateway: GoldenRecordTaskGateway,
    private val taskStepBuildService: TaskStepBuildService,
    private val trackingService: GoldenRecordTaskTrackingService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {

    private val logger = KotlinLogging.logger { }

    // Each task commits on its own and the remote calls stay outside, so entries commit through a template rather than @Transactional.
    private val entryTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordTaskResolutionApplicationService::class.qualifiedName}.resolveTasks")
    }

    /**
     * Resolves every task the Orchestrator holds for the Pool, one reservation at a time, until none is left.
     */
    fun resolveTasks() {
        var totalTasksProcessed = 0
        do {
            val reservedTasks = taskGateway.reserveTasks(taskProperties.batchSize)
            if (reservedTasks.isNotEmpty()) resolveReservation(reservedTasks)
            totalTasksProcessed += reservedTasks.size
        } while (reservedTasks.isNotEmpty())

        logger.debug { "Processed $totalTasksProcessed golden record tasks" }
    }

    private fun resolveReservation(reservedTasks: List<TaskStepReservationEntryDto>) {
        try {
            taskGateway.resolveTasks(reservedTasks.map(::resolve))
        } catch (ex: Throwable) {
            logger.error(ex) { "Error while processing cleaning task" }
            trackingService.trackUnreported(reservedTasks.map { it.taskId })
        }
    }

    private fun resolve(task: TaskStepReservationEntryDto): TaskStepResultEntryDto =
        try {
            entryTransaction.execute { taskStepBuildService.upsertBusinessPartner(task) }!!
        } catch (ex: BpdmValidationException) {
            toRejected(task, listOf(ex.message ?: ""))
        } catch (ex: BpdmMultiValidationException) {
            toRejected(task, ex.validationErrors)
        } catch (ex: Throwable) {
            logger.error(ex) { "An unexpected error occurred during golden record task processing" }
            toRejected(task, listOf("An unexpected error occurred during Pool update"))
        }

    private fun toRejected(task: TaskStepReservationEntryDto, errorDescriptions: List<String>) =
        TaskStepResultEntryDto(
            taskId = task.taskId,
            businessPartner = task.businessPartner,
            errors = errorDescriptions.map { TaskErrorDto(type = TaskErrorType.Unspecified, description = it) }
        )
}
