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
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.OrchestratorInboundMapper
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.OrchestratorOutboundMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.task.CreatedGoldenRecordTaskRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.FinishedGoldenRecordTaskPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResultRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.TaskReferenceRaw
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestrationApiClient
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Exchanges the golden record tasks of business partners with the Orchestrator.
 */
@Component
class GoldenRecordTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val orchestratorOutboundMapper: OrchestratorOutboundMapper,
    private val orchestratorInboundMapper: OrchestratorInboundMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties
) {

    /**
     * Creates a golden record task for each of [candidates] and returns the created tasks in the order of [candidates].
     */
    fun createTasks(candidates: List<GoldenRecordTaskCandidateParsed>): List<CreatedGoldenRecordTaskRaw> {
        val createdTasks = orchestrationApiClient.goldenRecordTasks.createTasks(orchestratorOutboundMapper.toCreateRequest(candidates)).createdTasks
        check(createdTasks.size == candidates.size) { "The Orchestrator created ${createdTasks.size} golden record tasks for ${candidates.size} requested" }
        return createdTasks.map { CreatedGoldenRecordTaskRaw(taskId = it.taskId, recordId = it.recordId) }
    }

    /**
     * Returns the oldest page of tasks the Orchestrator reports as finished since [since].
     */
    fun fetchFinishedTasks(since: Instant): FinishedGoldenRecordTaskPageRaw {
        val events = orchestrationApiClient.finishedTaskEvents.getEvents(since, PaginationRequest(0, EVENT_PAGE_SIZE))
        return orchestratorInboundMapper.toPage(events)
    }

    /**
     * Returns the current result of each task in [tasks], leaving out tasks the Orchestrator does not know.
     */
    fun fetchResults(tasks: List<TaskReferenceRaw>): List<GoldenRecordTaskResultRaw> =
        tasks.chunked(taskProperties.check.batchSize) { chunk ->
            orchestrationApiClient.goldenRecordTasks.searchTaskStates(orchestratorOutboundMapper.toRequest(chunk)).tasks
        }.flatten().map(orchestratorInboundMapper::toRequest)

    /**
     * Returns those of [taskIds] the Orchestrator knows.
     */
    fun fetchKnownTaskIds(taskIds: Collection<String>): Set<String> =
        taskIds.distinct().chunked(taskProperties.healthCheck.batchSize) { chunk ->
            val resultStates = orchestrationApiClient.goldenRecordTasks.searchTaskResultStates(orchestratorOutboundMapper.toResultStateSearchRequest(chunk)).resultStates
            check(resultStates.size == chunk.size) { "The Orchestrator returned ${resultStates.size} task result states for ${chunk.size} requested" }
            chunk.zip(resultStates).filter { (_, resultState) -> resultState != null }.map { (taskId, _) -> taskId }
        }.flatten().toSet()

    companion object {
        private const val EVENT_PAGE_SIZE = 1000
    }
}
