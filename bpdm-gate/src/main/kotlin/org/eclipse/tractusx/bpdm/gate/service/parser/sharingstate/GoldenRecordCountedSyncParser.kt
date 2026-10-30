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

package org.eclipse.tractusx.bpdm.gate.service.parser.sharingstate

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.GoldenRecordCountedSyncError
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.GoldenRecordCountedSyncParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.GoldenRecordCountedUpdateRaw
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.stereotype.Service

/**
 * Resolves the business partners whose golden record counted flag was sent to the Orchestrator.
 */
@Service
class GoldenRecordCountedSyncParser(
    private val sharingStateRepository: SharingStateRepository
) {

    /**
     * Resolves the sharing state of each sent update in [updates], failing those whose sharing state no longer exists.
     */
    fun parse(updates: List<GoldenRecordCountedUpdateRaw>): List<ParseResult<GoldenRecordCountedSyncParsed, GoldenRecordCountedSyncError>> {
        val sharingStatesById = sharingStateRepository.findAllById(updates.map { it.sharingStateId }).associateBy { it.id }
        return updates.map { update ->
            val sharingState = sharingStatesById[update.sharingStateId]
                ?: return@map ParseResult.ofSingleFailure(GoldenRecordCountedSyncError.SharingStateMissing(update.sharingStateId))
            ParseResult.Success(GoldenRecordCountedSyncParsed(sharingState, update.isGoldenRecordCounted))
        }
    }
}
