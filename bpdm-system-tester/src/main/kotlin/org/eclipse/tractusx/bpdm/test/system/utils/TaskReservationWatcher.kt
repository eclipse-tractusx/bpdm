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
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationRequest
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TaskReservationWatcher(
    private val orchestrationApiClient: OrchestrationApiClient
) {

    companion object {
        private val WAIT_TIMEOUT = Duration.ofMinutes(4)
        private const val POLL_INTERVAL_SECONDS = 10L
        private const val RESERVATION_AMOUNT = 10
    }

    // already-reserved entries keyed by taskId — populated by the scheduler
    private val reservedTasks  = ConcurrentHashMap<String, TaskStepReservationEntryDto>()
    // futures for callers that are waiting on a not-yet-reserved taskId
    private val waitingFutures = ConcurrentHashMap<String, CompletableFuture<TaskStepReservationEntryDto>>()

    private val reservedRelationTasks  = ConcurrentHashMap<String, TaskRelationsStepReservationEntryDto>()
    private val waitingRelationFutures = ConcurrentHashMap<String, CompletableFuture<TaskRelationsStepReservationEntryDto>>()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "task-reservation-watcher").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleWithFixedDelay(::poll, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    fun waitForReservedTask(taskId: String): TaskStepReservationEntryDto {
        // Fast path: task already reserved before this call
        reservedTasks[taskId]?.let { return it }

        // Register interest, then re-check to close the window between the read above
        // and the write below — the scheduler may have stored the entry in between
        val future = waitingFutures.computeIfAbsent(taskId) { CompletableFuture() }
        reservedTasks[taskId]?.let {
            waitingFutures.remove(taskId)
            return it
        }

        return try {
            future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            waitingFutures.remove(taskId)
            throw TimeoutException("No CleanAndSync task was reserved for record '$taskId' within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    fun waitForReservedRelationTask(taskId: String): TaskRelationsStepReservationEntryDto {
        reservedRelationTasks[taskId]?.let { return it }

        val future = waitingRelationFutures.computeIfAbsent(taskId) { CompletableFuture() }
        reservedRelationTasks[taskId]?.let {
            waitingRelationFutures.remove(taskId)
            return it
        }

        return try {
            future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            waitingRelationFutures.remove(taskId)
            throw TimeoutException("No CleanAndSync relation task was reserved for record '$taskId' within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    private fun poll() {
        if (waitingFutures.isEmpty() && waitingRelationFutures.isEmpty()) return

        try {
            if (waitingFutures.isNotEmpty()) {
                val response = orchestrationApiClient.goldenRecordTasks.reserveTasksForStep(
                    TaskStepReservationRequest(RESERVATION_AMOUNT, TaskStep.CleanAndSync)
                )
                response.reservedTasks.forEach { entry ->
                    reservedTasks[entry.taskId] = entry
                    waitingFutures.remove(entry.taskId)?.complete(entry)
                }
            }
        } catch (_: Exception) {
            // transient errors must not kill the scheduler thread; next poll will retry
        }

        try {
            if (waitingRelationFutures.isNotEmpty()) {
                val response = orchestrationApiClient.relationsGoldenRecordTasks.reserveTasksForStep(
                    TaskStepReservationRequest(RESERVATION_AMOUNT, TaskStep.CleanAndSync)
                )
                response.reservedTasks.forEach { entry ->
                    reservedRelationTasks[entry.taskId] = entry
                    waitingRelationFutures.remove(entry.taskId)?.complete(entry)
                }
            }
        } catch (_: Exception) {
            // transient errors must not kill the scheduler thread; next poll will retry
        }
    }
}
