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

package org.eclipse.tractusx.bpdm.pool.service.gateway

import org.eclipse.tractusx.bpdm.pool.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.OrchestratorInboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.OrchestratorOutboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordTaskRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.TaskErrorReportOutcome
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepResultRequest
import org.springframework.stereotype.Component

/**
 * Exchanges the golden record tasks of the Pool's task step with the Orchestrator.
 */
@Component
class GoldenRecordTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    private val inboundMapper: OrchestratorInboundMapper,
    private val outboundMapper: OrchestratorOutboundMapper
) {

    /**
     * Reserves up to [amount] golden record tasks waiting for the Pool and returns them, or none when no task waits.
     */
    fun reserveTasks(amount: Int): List<GoldenRecordTaskRaw> =
        orchestrationApiClient.goldenRecordTasks
            .reserveTasksForStep(TaskStepReservationRequest(step = taskProperties.step, amount = amount))
            .reservedTasks
            .map { inboundMapper.toRaw(it) }

    /**
     * Reports [results] of reserved golden record tasks to the Orchestrator.
     */
    fun resolveTasks(results: List<GoldenRecordTaskResultRecord>) {
        val resultEntries = results.map { withAcceptedErrorLength(outboundMapper.toResultEntry(it)) }
        orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step = taskProperties.step, results = resultEntries))
    }

    /**
     * Reports the tasks with [taskIds] to the Orchestrator as failed without a known cause and returns how it answered each.
     */
    fun resolveTasksAsErrors(taskIds: List<String>): List<TaskErrorReportOutcome> =
        reportTaskErrors(taskIds) { taskId ->
            val resultEntry = outboundMapper.toErrorReportEntry(taskId)
            orchestrationApiClient.goldenRecordTasks.resolveStepResults(TaskStepResultRequest(step = taskProperties.step, results = listOf(resultEntry)))
        }

    private fun withAcceptedErrorLength(result: TaskStepResultEntryDto) =
        result.copy(errors = result.errors.map { it.copy(description = it.description.take(MAX_ERROR_DESCRIPTION_LENGTH)) })
}
