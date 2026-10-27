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

import org.eclipse.tractusx.bpdm.gate.model.task.CreatedRelationTaskRequest
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskCandidate
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Component

/**
 * Exchanges the golden record tasks of relations with the Orchestrator.
 */
@Component
class RelationTaskGateway(
    private val orchestrationApiClient: OrchestrationApiClient
) {

    /**
     * Creates a golden record task for each of [candidates] and returns the created tasks in the order of [candidates].
     */
    fun createTasks(candidates: List<RelationTaskCandidate>): List<CreatedRelationTaskRequest> {
        val entries = candidates.map { TaskCreateRelationsRequestEntry(recordId = it.recordId, businessPartnerRelations = it.businessPartnerRelations) }
        val createdTasks = orchestrationApiClient.relationsGoldenRecordTasks.createTasks(TaskCreateRelationsRequest(TaskMode.UpdateFromSharingMember, entries)).createdTasks
        check(createdTasks.size == candidates.size) { "The Orchestrator created ${createdTasks.size} relation tasks for ${candidates.size} requested" }
        return createdTasks.map { CreatedRelationTaskRequest(taskId = it.taskId, recordId = it.recordId) }
    }
}
