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
        private val COMPLETED_STATES = setOf(SharingStateType.Success, SharingStateType.Error, SharingStateType.Initial)
        private val WAIT_TIMEOUT = Duration.ofMinutes(4)
        private const val POLL_INTERVAL_SECONDS = 10L
        private const val PAGE_SIZE = 100
    }

    private val awaitingCompletedState = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()
    private val awaitingPendingState   = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sharing-state-watcher").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleWithFixedDelay(::poll, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    fun waitForCompletedState(externalId: String): SharingStateType {
        val future = awaitingCompletedState.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            awaitingCompletedState.remove(externalId)
            throw TimeoutException("Sharing state for '$externalId' did not reach a completed state within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    fun waitForPendingState(externalId: String): SharingStateType {
        val future = awaitingPendingState.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            awaitingPendingState.remove(externalId)
            throw TimeoutException("Sharing state for '$externalId' did not reach pending state within ${WAIT_TIMEOUT.toMinutes()} minutes")
        }
    }

    private fun poll() {
        val externalIds = (awaitingCompletedState.keys + awaitingPendingState.keys).distinct()
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
                    if (state.sharingStateType in COMPLETED_STATES) {
                        awaitingCompletedState.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                    if (state.sharingStateType == SharingStateType.Pending) {
                        awaitingPendingState.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                }
                fetched += response.content.size
                page++
            } while (fetched < response.totalElements)
        } catch (_: Exception) {
            // transient errors must not kill the scheduler thread; next poll will retry
        }
    }
}
