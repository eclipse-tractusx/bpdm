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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared test context between all scenarios
 */
@Component
class TestRepository(
    private val orchestratorClient: OrchestrationApiClient
) {
    /**
     * Task-IDs are on-the-fly by the golden record process.
     * Additionally, as a refinement service we can't reserve a specific task for processing from the golden record process.
     * Therefore, we just reserve everything and store it for later use by the Cucumber steps here
     */
    private val reservedTasksById: ConcurrentHashMap<String, TaskRelationsStepReservationEntryDto> = ConcurrentHashMap()

    /**
     * Reserve all tasks in queue up to this point in time and store the reserved tasks for later use
     *
     * Reservation of tasks in the orchestrator and storing these tasks needs to be an atomic operation
     */
    fun reserveTasks(){
        synchronized(reservedTasksById) {
            var reservedTasks: List<TaskRelationsStepReservationEntryDto> = emptyList()
            do{
                reservedTasks = orchestratorClient.relationsGoldenRecordTasks.reserveTasksForStep(TaskStepReservationRequest(100, TaskStep.CleanAndSync)).reservedTasks
                reservedTasks.forEach { task -> reservedTasksById.putIfAbsent(task.taskId, task) }
            }while (reservedTasks.isNotEmpty())
        }
    }

    fun getReservedTask(taskId: String): TaskRelationsStepReservationEntryDto{
        synchronized(reservedTasksById){
            return reservedTasksById[taskId]!!
        }
    }
}