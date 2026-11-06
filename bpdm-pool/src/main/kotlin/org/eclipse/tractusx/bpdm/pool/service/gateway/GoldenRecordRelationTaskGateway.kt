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
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound.OrchestratorRelationOutboundMapper
import org.eclipse.tractusx.bpdm.pool.model.output.task.GoldenRecordRelationTaskResultRecord
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRelationTaskRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.TaskErrorReportOutcome
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.springframework.stereotype.Component

/**
 * Exchanges the golden record relation tasks of the Pool's task step with the Orchestrator.
 */
@Component
class GoldenRecordRelationTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    private val inboundMapper: OrchestratorInboundMapper,
    private val outboundMapper: OrchestratorRelationOutboundMapper
) {

    /**
     * Reserves up to [amount] golden record relation tasks waiting for the Pool and returns them, or none when no task waits.
     */
    fun reserveTasks(amount: Int): List<GoldenRecordRelationTaskRaw> =
        orchestrationApiClient.relationsGoldenRecordTasks
            .reserveTasksForStep(TaskStepReservationRequest(step = taskProperties.step, amount = amount))
            .reservedTasks
            .map { inboundMapper.toRaw(it) }

    /**
     * Reports [results] of reserved golden record relation tasks to the Orchestrator.
     */
    fun resolveTasks(results: List<GoldenRecordRelationTaskResultRecord>) {
        val resultEntries = results.map { withAcceptedErrorLength(outboundMapper.toResultEntry(it)) }
        orchestrationApiClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(step = taskProperties.step, results = resultEntries))
    }

    /**
     * Reports the relation tasks with [taskIds] to the Orchestrator as failed without a known cause and returns how it answered each.
     */
    fun resolveTasksAsErrors(taskIds: List<String>): List<TaskErrorReportOutcome> =
        reportTaskErrors(taskIds) { taskId ->
            val resultEntry = outboundMapper.toErrorReportEntry(taskId)
            orchestrationApiClient.relationsGoldenRecordTasks.resolveStepResults(TaskRelationsStepResultRequest(step = taskProperties.step, results = listOf(resultEntry)))
        }

    private fun withAcceptedErrorLength(result: TaskRelationsStepResultEntryDto) =
        result.copy(errors = result.errors.map { it.copy(description = it.description.take(MAX_ERROR_DESCRIPTION_LENGTH)) })
}
