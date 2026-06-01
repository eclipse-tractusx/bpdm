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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SharingStateWatcher(
    private val gateClient: GateClient
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        // Success is completed after a fixed delay (see CONFIDENCE_SYNC_DELAY_SECONDS) rather
        // than immediately, because the golden record process may silently adjust confidence
        // criteria for up to 30 seconds after reporting Success.
        private val TERMINAL_STATES = setOf(SharingStateType.Error, SharingStateType.Initial)
        private val WAIT_TIMEOUT = Duration.ofMinutes(4)
        private const val POLL_INTERVAL_SECONDS = 10L
        private const val CONFIDENCE_SYNC_DELAY_SECONDS = 35L
        private const val PAGE_SIZE = 100
    }

    private val awaitingCompletedState = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()
    private val awaitingTaskId         = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sharing-state-watcher").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleWithFixedDelay(::poll, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    fun waitForCompletedState(externalId: String): SharingStateType {
        logger.info { "Waiting for completed sharing state of '$externalId'" }
        val future = awaitingCompletedState.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "Sharing state of '$externalId' completed with: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingCompletedState.remove(externalId)
            throw TimeoutException("Sharing state for '$externalId' did not reach a completed state within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    fun waitForTaskId(externalId: String): SharingStateType {
        logger.info { "Waiting for task ID assignment of '$externalId'" }
        val future = awaitingTaskId.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "Task ID assigned for '$externalId', sharing state is now: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingTaskId.remove(externalId)
            throw TimeoutException("Sharing state for '$externalId' did not receive a task ID within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    private fun poll() {
        val externalIds = (awaitingCompletedState.keys + awaitingTaskId.keys).distinct()
        if (externalIds.isEmpty()) return

        try {
            var page = 0
            var fetched = 0
            do {
                val response = gateClient.sharingState.getSharingStates(
                    PaginationRequest(page, PAGE_SIZE),
                    externalIds
                )
                response.content.forEach { state ->
                    if (state.sharingStateType == SharingStateType.Success) {
                        val future = awaitingCompletedState.remove(state.externalId)
                        if (future != null) {
                            scheduler.schedule(
                                { future.complete(SharingStateType.Success) },
                                CONFIDENCE_SYNC_DELAY_SECONDS,
                                TimeUnit.SECONDS
                            )
                        }
                    } else if (state.sharingStateType in TERMINAL_STATES) {
                        awaitingCompletedState.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                    if (state.taskId != null || state.sharingStateType == SharingStateType.Error || state.sharingStateType == SharingStateType.Success) {
                        awaitingTaskId.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                }
                fetched += response.content.size
                page++
            } while (fetched < response.totalElements)
        } catch (e: Exception) {
            logger.warn(e) { "Poll failed, will retry in ${POLL_INTERVAL_SECONDS}s" }
        }
    }
}
