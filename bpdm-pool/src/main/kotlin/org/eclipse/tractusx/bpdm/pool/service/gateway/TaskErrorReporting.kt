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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.model.raw.task.TaskErrorReportOutcomeRaw
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger { }

/**
 * The longest task error description the Orchestrator accepts.
 */
internal const val MAX_ERROR_DESCRIPTION_LENGTH = 250

/**
 * Reports each task with [taskIds] through [report] and returns how the Orchestrator answered each.
 */
internal fun reportTaskErrors(taskIds: List<String>, report: (String) -> Unit): List<TaskErrorReportOutcomeRaw> =
    // One call per task: the Orchestrator rejects a whole result batch when it does not know one of its tasks.
    taskIds.map { taskId ->
        try {
            report(taskId)
            TaskErrorReportOutcomeRaw.Reported
        } catch (_: WebClientResponseException.BadRequest) {
            TaskErrorReportOutcomeRaw.TaskUnknown
        } catch (e: WebClientException) {
            logger.debug(e) { "Could not report task '$taskId' as an error" }
            TaskErrorReportOutcomeRaw.Failed(e.message ?: e::class.simpleName.orEmpty())
        }
    }
