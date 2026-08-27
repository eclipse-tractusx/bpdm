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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SharingStateWatcher(
    private val gateClient: GateClient,
    member: SharingMember
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        // Success is completed after a fixed delay (see CONFIDENCE_SYNC_DELAY_SECONDS) rather
        // than immediately, because the golden record process may silently adjust confidence
        // criteria for up to 30 seconds after reporting Success.
        private val TERMINAL_STATES = setOf(SharingStateType.Error, SharingStateType.Initial)
        private val RELATION_TERMINAL_STATES = setOf(RelationSharingStateType.Error, RelationSharingStateType.Ready)
        private val WAIT_TIMEOUT = Duration.ofMinutes(6)
        private const val POLL_INTERVAL_SECONDS = 10L
        private const val CONFIDENCE_SYNC_DELAY_SECONDS = 35L
        private const val PAGE_SIZE = 100
    }

    private val awaitingCompletedState         = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()
    private val awaitingTaskId                 = ConcurrentHashMap<String, CompletableFuture<SharingStateType>>()
    private val awaitingRelationCompletedState = ConcurrentHashMap<String, CompletableFuture<RelationSharingStateType>>()
    private val awaitingRelationTaskId         = ConcurrentHashMap<String, CompletableFuture<RelationSharingStateType>>()

    private val memberName = member.name.lowercase()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sharing-state-watcher-$memberName").also { it.isDaemon = true }
    }

    init {
        scheduler.scheduleWithFixedDelay(::poll, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    fun waitForCompletedState(recordId: String): SharingStateType {
        val scenario = ScenarioContext.current()?.scenarioName
        val externalId = ScenarioContext.current()!!.runId(recordId)
        logger.info { "[$scenario] Waiting for completed sharing state of '$recordId'" }
        val future = awaitingCompletedState.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "[$scenario] Sharing state of '$recordId' completed with: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingCompletedState.remove(externalId)
            throw TimeoutException(
                "Sharing state for '$recordId' at the $memberName sharing member's Gate did not reach a completed state" +
                        " within ${WAIT_TIMEOUT.toMinutes()} minutes"
            )
        }
    }

    fun waitForTaskId(recordId: String): SharingStateType {
        val scenario = ScenarioContext.current()?.scenarioName
        val externalId = ScenarioContext.current()!!.runId(recordId)
        logger.info { "[$scenario] Waiting for task ID assignment of '$recordId'" }
        val future = awaitingTaskId.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "[$scenario] Task ID assigned for '$recordId', sharing state is now: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingTaskId.remove(externalId)
            throw TimeoutException(
                "Sharing state for '$recordId' at the $memberName sharing member's Gate did not receive a task ID" +
                        " within ${WAIT_TIMEOUT.toMinutes()} minutes"
            )
        }
    }

    fun waitForRelationCompletedState(recordId: String): RelationSharingStateType {
        val scenario = ScenarioContext.current()?.scenarioName
        val externalId = ScenarioContext.current()!!.runId(recordId)
        logger.info { "[$scenario] Waiting for completed relation sharing state of '$recordId'" }
        val future = awaitingRelationCompletedState.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "[$scenario] Relation sharing state of '$recordId' completed with: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingRelationCompletedState.remove(externalId)
            throw TimeoutException(
                "Relation sharing state for '$recordId' at the $memberName sharing member's Gate did not reach a" +
                        " completed state within ${WAIT_TIMEOUT.toMinutes()} minutes"
            )
        }
    }

    fun waitForRelationTaskId(recordId: String): RelationSharingStateType {
        val scenario = ScenarioContext.current()?.scenarioName
        val externalId = ScenarioContext.current()!!.runId(recordId)
        logger.info { "[$scenario] Waiting for task ID assignment of relation '$recordId'" }
        val future = awaitingRelationTaskId.computeIfAbsent(externalId) { CompletableFuture() }
        return try {
            val result = future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
            logger.info { "[$scenario] Task ID assigned for relation '$recordId', sharing state is now: $result" }
            result
        } catch (e: TimeoutException) {
            awaitingRelationTaskId.remove(externalId)
            throw TimeoutException(
                "Relation sharing state for '$recordId' at the $memberName sharing member's Gate did not receive a" +
                        " task ID within ${WAIT_TIMEOUT.toMinutes()} minutes"
            )
        }
    }

    private fun poll() {
        pollBusinessPartnerSharingStates()
        pollRelationSharingStates()
    }

    private fun pollBusinessPartnerSharingStates() {
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
            logger.warn(e) { "BP sharing state poll failed, will retry in ${POLL_INTERVAL_SECONDS}s" }
        }
    }

    private fun pollRelationSharingStates() {
        val externalIds = (awaitingRelationCompletedState.keys + awaitingRelationTaskId.keys).distinct()
        if (externalIds.isEmpty()) return

        try {
            var page = 0
            var fetched = 0
            do {
                val response = gateClient.relationSharingState.get(
                    externalIds = externalIds,
                    sharingStateTypes = null,
                    updatedAfter = null,
                    paginationRequest = PaginationRequest(page, PAGE_SIZE)
                )
                response.content.forEach { state ->
                    if (state.sharingStateType == RelationSharingStateType.Success) {
                        val future = awaitingRelationCompletedState.remove(state.externalId)
                        if (future != null) {
                            scheduler.schedule(
                                { future.complete(RelationSharingStateType.Success) },
                                CONFIDENCE_SYNC_DELAY_SECONDS,
                                TimeUnit.SECONDS
                            )
                        }
                    } else if (state.sharingStateType in RELATION_TERMINAL_STATES) {
                        awaitingRelationCompletedState.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                    if (state.taskId != null || state.sharingStateType == RelationSharingStateType.Error || state.sharingStateType == RelationSharingStateType.Success) {
                        awaitingRelationTaskId.remove(state.externalId)?.complete(state.sharingStateType)
                    }
                }
                fetched += response.content.size
                page++
            } while (fetched < response.totalElements)
        } catch (e: Exception) {
            logger.warn(e) { "Relation sharing state poll failed, will retry in ${POLL_INTERVAL_SECONDS}s" }
        }
    }
}
