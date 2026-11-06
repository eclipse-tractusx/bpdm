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
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskDb
import org.eclipse.tractusx.bpdm.pool.entity.GoldenRecordTaskKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.TaskErrorReportOutcomeRaw
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordRelationTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.pool.service.operation.task.GoldenRecordTaskTrackingService
import org.eclipse.tractusx.bpdm.pool.util.countTasksForLog
import org.springframework.stereotype.Service

/**
 * Reports the tracked tasks whose result never reached the Orchestrator as errors, so they do not wait for its timeout.
 */
@Service
class UnreportedTaskReportApplicationService(
    private val trackingService: GoldenRecordTaskTrackingService,
    private val taskGateway: GoldenRecordTaskGateway,
    private val relationTaskGateway: GoldenRecordRelationTaskGateway
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Reports every unreported task of every kind as an error and stops tracking those the Orchestrator no longer waits for.
     */
    fun reportUnreportedTasks() {
        GoldenRecordTaskKind.entries.forEach(::reportUnreportedTasks)
    }

    private fun reportUnreportedTasks(kind: GoldenRecordTaskKind) {
        val unreportedTasks = trackingService.findUnreported(kind)
        if (unreportedTasks.isEmpty()) {
            logger.debug { "Found ${countTasksForLog(0, kind)} to report as errors" }
            return
        }

        val taskIds = unreportedTasks.map { it.taskId }
        val outcomes = when (kind) {
            GoldenRecordTaskKind.BusinessPartner -> taskGateway.resolveTasksAsErrors(taskIds)
            GoldenRecordTaskKind.Relations -> relationTaskGateway.resolveTasksAsErrors(taskIds)
        }
        val tasksWithOutcome = unreportedTasks.zip(outcomes)

        val unknownTasks = tasksWithOutcome.filter { (_, outcome) -> outcome is TaskErrorReportOutcomeRaw.TaskUnknown }.map { (task, _) -> task }
        if (unknownTasks.isNotEmpty())
            logger.warn { "Orchestrator does not know ${countTasksForLog(unknownTasks.size, kind)}, dropping them: ${unknownTasks.toLogIdentifiers()}" }

        val failedReports = tasksWithOutcome.mapNotNull { (task, outcome) -> (outcome as? TaskErrorReportOutcomeRaw.Failed)?.let { task to it.cause } }
        if (failedReports.isNotEmpty())
            logger.warn {
                "Could not report ${countTasksForLog(failedReports.size, kind)} as errors, retrying in the next run: " +
                    "${failedReports.map { (task, _) -> task.taskId }.joinIdentifiersForLog()} (causes: ${failedReports.map { (_, cause) -> cause }.distinct().joinIdentifiersForLog()})"
            }

        val reportedTasks = tasksWithOutcome.filter { (_, outcome) -> outcome is TaskErrorReportOutcomeRaw.Reported }.map { (task, _) -> task }
        trackingService.untrack(reportedTasks + unknownTasks)
    }

    private fun List<GoldenRecordTaskDb>.toLogIdentifiers() = map { it.taskId }.joinIdentifiersForLog()
}
