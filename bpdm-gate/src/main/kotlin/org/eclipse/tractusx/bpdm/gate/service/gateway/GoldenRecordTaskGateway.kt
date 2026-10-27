/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.gateway

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.inbound.GoldenRecordTaskResultRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.task.CreatedGoldenRecordTaskRequest
import org.eclipse.tractusx.bpdm.gate.model.task.FinishedGoldenRecordTaskPage
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResultRequest
import org.eclipse.tractusx.bpdm.gate.model.task.TaskReference
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Exchanges the golden record tasks of business partners with the Orchestrator.
 */
@Component
class GoldenRecordTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val requestMapper: GoldenRecordTaskResultRequestMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties
) {

    /**
     * Creates a golden record task for each of [candidates] and returns the created tasks in the order of [candidates].
     */
    fun createTasks(candidates: List<GoldenRecordTaskCandidate>): List<CreatedGoldenRecordTaskRequest> {
        val entries = candidates.map { TaskCreateRequestEntry(recordId = it.recordId, businessPartner = it.businessPartner) }
        val createdTasks = orchestrationApiClient.goldenRecordTasks.createTasks(TaskCreateRequest(TaskMode.UpdateFromSharingMember, entries)).createdTasks
        check(createdTasks.size == candidates.size) { "The Orchestrator created ${createdTasks.size} golden record tasks for ${candidates.size} requested" }
        return createdTasks.map { CreatedGoldenRecordTaskRequest(taskId = it.taskId, recordId = it.recordId) }
    }

    /**
     * Returns the oldest page of tasks the Orchestrator reports as finished since [since].
     */
    fun fetchFinishedTasks(since: Instant): FinishedGoldenRecordTaskPage {
        val events = orchestrationApiClient.finishedTaskEvents.getEvents(since, PaginationRequest(0, EVENT_PAGE_SIZE))
        return FinishedGoldenRecordTaskPage(
            taskIds = events.content.map { it.taskId },
            lastFinishedAt = events.content.lastOrNull()?.timestamp,
            hasMore = events.totalPages > 1
        )
    }

    /**
     * Returns the current result of each task in [tasks], leaving out tasks the Orchestrator does not know.
     */
    fun fetchResults(tasks: List<TaskReference>): List<GoldenRecordTaskResultRequest> =
        tasks.chunked(taskProperties.check.batchSize) { chunk ->
            val entries = chunk.map { TaskStateRequest.Entry(taskId = it.taskId, recordId = it.recordId) }
            orchestrationApiClient.goldenRecordTasks.searchTaskStates(TaskStateRequest(entries)).tasks
        }.flatten().map(requestMapper::toRequest)

    companion object {
        private const val EVENT_PAGE_SIZE = 1000
    }
}
