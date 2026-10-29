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
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputContentMapper
import org.eclipse.tractusx.bpdm.gate.model.task.CreatedGoldenRecordTaskRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationParsed
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Decides which business partners start the golden record tasks the Orchestrator created for them.
 */
@Service
class GoldenRecordTaskCreationParser(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val contentMapper: BusinessPartnerInputContentMapper
) {

    /**
     * Starts the created task of each candidate in [page] that is still ready to be shared with the input the task was
     * created from, and marks every other candidate as stale.
     */
    @Transactional(readOnly = true)
    fun parse(page: GoldenRecordTaskCreationPageRequest): List<ParseResult<GoldenRecordTaskCreationParsed, GoldenRecordTaskCreationError>> {
        val sharingStatesById = sharingStateRepository.findAllById(page.candidates.map { it.sharingStateId }).associateBy { it.id }
        val inputsBySharingStateId = businessPartnerRepository.findBySharingStateInAndStage(sharingStatesById.values, StageType.Input)
            .associateBy { it.sharingState.id }

        return page.candidates.zip(page.createdTasks) { candidate, createdTask ->
            parse(candidate, createdTask, sharingStatesById[candidate.sharingStateId], inputsBySharingStateId[candidate.sharingStateId])
        }
    }

    private fun parse(
        candidate: GoldenRecordTaskCandidate,
        createdTask: CreatedGoldenRecordTaskRequest,
        sharingState: SharingStateDb?,
        input: BusinessPartnerDb?
    ): ParseResult<GoldenRecordTaskCreationParsed, GoldenRecordTaskCreationError> {
        if (sharingState == null)
            return ParseResult.ofSingleFailure(GoldenRecordTaskCreationError.SharingStateMissing(candidate.externalId, createdTask.taskId))

        val recordId = runCatching { UUID.fromString(createdTask.recordId) }.getOrNull()
            ?: return ParseResult.ofSingleFailure(
                GoldenRecordTaskCreationError.RecordIdInvalid(candidate.externalId, createdTask.taskId, createdTask.recordId)
            )
        val recordIdToAssign = recordId.takeIf { sharingState.orchestratorRecordId == null }

        val parsed = if (isUnchangedSince(candidate, sharingState, input))
            GoldenRecordTaskCreationParsed.Started(sharingState, createdTask.taskId, recordIdToAssign)
        else
            GoldenRecordTaskCreationParsed.Stale(sharingState, recordIdToAssign)
        return ParseResult.Success(parsed)
    }

    private fun isUnchangedSince(candidate: GoldenRecordTaskCandidate, sharingState: SharingStateDb, input: BusinessPartnerDb?): Boolean =
        sharingState.sharingStateType == SharingStateType.Ready
                && sharingState.orchestratorRecordId?.toString() == candidate.recordId
                && input != null
                && contentMapper.toContent(input) == candidate.input
}
