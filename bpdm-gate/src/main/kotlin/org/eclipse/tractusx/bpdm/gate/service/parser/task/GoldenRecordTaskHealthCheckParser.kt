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
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckParsed
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.stereotype.Service

/**
 * Resolves the business partners whose golden record task was checked against the Orchestrator.
 */
@Service
class GoldenRecordTaskHealthCheckParser(
    private val sharingStateRepository: SharingStateRepository
) {

    /**
     * Resolves the sharing state of each checked task in [page], failing those whose sharing state no longer exists.
     */
    fun parse(page: GoldenRecordTaskHealthCheckPageRequest): List<ParseResult<GoldenRecordTaskHealthCheckParsed, GoldenRecordTaskHealthCheckError>> {
        val sharingStatesById = sharingStateRepository.findAllById(page.pendingTasks.map { it.sharingStateId }).associateBy { it.id }
        return page.pendingTasks.map { pendingTask ->
            val sharingState = sharingStatesById[pendingTask.sharingStateId]
                ?: return@map ParseResult.ofSingleFailure(GoldenRecordTaskHealthCheckError.SharingStateMissing(pendingTask.sharingStateId, pendingTask.taskId))
            ParseResult.Success(GoldenRecordTaskHealthCheckParsed(sharingState, pendingTask.taskId, pendingTask.taskId in page.knownTaskIds))
        }
    }
}
