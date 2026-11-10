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
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.task.CreatedRelationTaskRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.FinishedGoldenRecordTaskPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.RelationTaskResultRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.TaskReferenceRaw
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.client.OrchestratorV7Client
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Exchanges the golden record tasks of relations with the Orchestrator.
 */
@Component
class RelationTaskGateway(
    private val orchestrationApiClient: OrchestratorV7Client,
    private val orchestratorOutboundMapper: OrchestratorOutboundMapper,
    private val orchestratorInboundMapper: OrchestratorInboundMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties
) {

    /**
     * Creates a golden record task for each of [candidates] and returns the created tasks in the order of [candidates].
     */
    fun createTasks(candidates: List<RelationTaskCandidateParsed>): List<CreatedRelationTaskRaw> {
        val createdTasks = orchestrationApiClient.relationsGoldenRecordTasks.createTasks(orchestratorOutboundMapper.toCreateRequest(candidates)).createdTasks
        check(createdTasks.size == candidates.size) { "The Orchestrator created ${createdTasks.size} relation tasks for ${candidates.size} requested" }
        return createdTasks.map { CreatedRelationTaskRaw(taskId = it.taskId, recordId = it.recordId) }
    }

    /**
     * Returns the oldest page of relation tasks the Orchestrator reports as finished since [since].
     */
    fun fetchFinishedTasks(since: Instant): FinishedGoldenRecordTaskPageRaw {
        val events = orchestrationApiClient.relationsFinishedTaskEvents.getRelationsEvents(since, PaginationRequest(0, EVENT_PAGE_SIZE))
        return orchestratorInboundMapper.toPage(events)
    }

    /**
     * Returns the current result of each task in [tasks], leaving out tasks the Orchestrator does not know.
     */
    fun fetchResults(tasks: List<TaskReferenceRaw>): List<RelationTaskResultRaw> =
        tasks.chunked(taskProperties.relationCheck.batchSize) { chunk ->
            orchestrationApiClient.relationsGoldenRecordTasks.searchTaskStates(orchestratorOutboundMapper.toRequest(chunk)).tasks
        }.flatten().map(orchestratorInboundMapper::toRequest)

    companion object {
        private const val EVENT_PAGE_SIZE = 1000
    }
}
