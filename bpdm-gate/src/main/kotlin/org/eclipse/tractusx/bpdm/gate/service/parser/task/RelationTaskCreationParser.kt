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

package org.eclipse.tractusx.bpdm.gate.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCreationError
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCreationParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.task.CreatedRelationTaskRaw
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which relations start the golden record tasks the Orchestrator created for them.
 */
@Service
class RelationTaskCreationParser(
    private val relationRepository: RelationRepository
) {

    /**
     * Starts the task in [createdTasks] created for the candidate at the same position in [candidates] if that candidate
     * is still staged for sharing as it was when the task was created.
     */
    @Transactional(readOnly = true)
    fun parse(candidates: List<RelationTaskCandidateParsed>, createdTasks: List<CreatedRelationTaskRaw>): List<ParseResult<RelationTaskCreationParsed, RelationTaskCreationError>> {
        val relationsById = relationRepository.findAllById(candidates.map { it.relationId }).associateBy { it.id }

        return candidates.zip(createdTasks) { candidate, createdTask ->
            parse(candidate, createdTask, relationsById[candidate.relationId])
        }
    }

    private fun parse(
        candidate: RelationTaskCandidateParsed,
        createdTask: CreatedRelationTaskRaw,
        relation: RelationDb?
    ): ParseResult<RelationTaskCreationParsed, RelationTaskCreationError> {
        if (relation == null)
            return ParseResult.ofSingleFailure(RelationTaskCreationError.RelationMissing(candidate.externalId, createdTask.taskId))
        if (!isUnchangedSince(candidate, relation))
            return ParseResult.ofSingleFailure(RelationTaskCreationError.RelationChanged(candidate.externalId, createdTask.taskId))

        return ParseResult.Success(RelationTaskCreationParsed(relation, createdTask.taskId, createdTask.recordId))
    }

    private fun isUnchangedSince(candidate: RelationTaskCandidateParsed, relation: RelationDb): Boolean {
        val sharingState = relation.sharingState ?: return false
        return sharingState.sharingStateType == RelationSharingStateType.Ready
                && sharingState.isStaged
                && sharingState.updatedAt == candidate.sharingStateUpdatedAt
    }
}
