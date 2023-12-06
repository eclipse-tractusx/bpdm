/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmDuplicateTaskIdException
import org.eclipse.tractusx.bpdm.orchestrator.model.GoldenRecordTask
import org.eclipse.tractusx.orchestrator.api.model.ResultState
import org.eclipse.tractusx.orchestrator.api.model.StepState
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GoldenRecordTaskStorage {

    private val tasks: MutableList<GoldenRecordTask> = mutableListOf()

    // Needed for testing
    fun clear() {
        tasks.clear()
    }

    fun addTask(task: GoldenRecordTask): GoldenRecordTask {
        return task.also {
            if (getTask(it.taskId) != null) {
                throw BpdmDuplicateTaskIdException(it.taskId)
            }
            tasks.add(it)
        }
    }

    fun removeTask(taskId: String) {
        tasks.removeIf {
            it.taskId == taskId
        }
    }

    fun getTask(taskId: String) =
        tasks.firstOrNull { it.taskId == taskId }

    fun getQueuedTasksByStep(step: TaskStep, amount: Int): List<GoldenRecordTask> =
        tasks
            .filter {
                val state = it.processingState
                state.resultState == ResultState.Pending &&
                        state.stepState == StepState.Queued &&
                        state.step == step
            }
            .take(amount)

    fun getTasksWithPendingTimeoutBefore(timestamp: Instant): List<GoldenRecordTask> =
        tasks
            .filter {
                val state = it.processingState
                state.taskPendingTimeout?.isBefore(timestamp) ?: false
            }

    fun getTasksWithRetentionTimeoutBefore(timestamp: Instant): List<GoldenRecordTask> =
        tasks
            .filter {
                val state = it.processingState
                state.taskRetentionTimeout?.isBefore(timestamp) ?: false
            }
}
