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
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.inbound.FinishedGoldenRecordTaskPageMapper
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.inbound.RelationTaskResultRequestMapper
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound.RelationTaskRequestMapper
import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound.TaskStateRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.task.CreatedRelationTaskRequest
import org.eclipse.tractusx.bpdm.gate.model.task.FinishedGoldenRecordTaskPage
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskCandidate
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResultRequest
import org.eclipse.tractusx.bpdm.gate.model.task.TaskReference
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Exchanges the golden record tasks of relations with the Orchestrator.
 */
@Component
class RelationTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val createRequestMapper: RelationTaskRequestMapper,
    private val stateRequestMapper: TaskStateRequestMapper,
    private val resultRequestMapper: RelationTaskResultRequestMapper,
    private val finishedPageMapper: FinishedGoldenRecordTaskPageMapper,
    private val taskProperties: GoldenRecordTaskConfigProperties
) {

    /**
     * Creates a golden record task for each of [candidates] and returns the created tasks in the order of [candidates].
     */
    fun createTasks(candidates: List<RelationTaskCandidate>): List<CreatedRelationTaskRequest> {
        val createdTasks = orchestrationApiClient.relationsGoldenRecordTasks.createTasks(createRequestMapper.toCreateRequest(candidates)).createdTasks
        check(createdTasks.size == candidates.size) { "The Orchestrator created ${createdTasks.size} relation tasks for ${candidates.size} requested" }
        return createdTasks.map { CreatedRelationTaskRequest(taskId = it.taskId, recordId = it.recordId) }
    }

    /**
     * Returns the oldest page of relation tasks the Orchestrator reports as finished since [since].
     */
    fun fetchFinishedTasks(since: Instant): FinishedGoldenRecordTaskPage {
        val events = orchestrationApiClient.relationsFinishedTaskEvents.getRelationsEvents(since, PaginationRequest(0, EVENT_PAGE_SIZE))
        return finishedPageMapper.toPage(events)
    }

    /**
     * Returns the current result of each task in [tasks], leaving out tasks the Orchestrator does not know.
     */
    fun fetchResults(tasks: List<TaskReference>): List<RelationTaskResultRequest> =
        tasks.chunked(taskProperties.relationCheck.batchSize) { chunk ->
            orchestrationApiClient.relationsGoldenRecordTasks.searchTaskStates(stateRequestMapper.toRequest(chunk)).tasks
        }.flatten().map(resultRequestMapper::toRequest)

    companion object {
        private const val EVENT_PAGE_SIZE = 1000
    }
}
