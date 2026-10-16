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
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.ready.SharingStateReadyError
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.ready.SharingStateReadyParsed
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.ready.SharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.stereotype.Service

/**
 * Resolves the sharing states a ready request names and decides whether each of them may be set ready.
 */
@Service
class SharingStateReadyParser(
    private val sharingStateRepository: SharingStateRepository
) {

    private val typesThatCanBeSetReady =setOf(SharingStateType.Initial, SharingStateType.Error)

    /**
     * Parses [request] for [tenantBpnl] as a whole, succeeding with the named sharing states only if every one of them
     * exists and is in an initial or error state, and failing with every violation otherwise.
     */
    fun parse(request: SharingStateReadyRequest, tenantBpnl: String?): ParseResult<SharingStateReadyParsed, SharingStateReadyError> {
        val externalIds = request.externalIds.distinct()
        val sharingStatesByExternalId = sharingStateRepository.findByExternalIdInAndTenantBpnl(externalIds, tenantBpnl)
            .associateBy { it.externalId }

        val errors = externalIds.mapNotNull { externalId ->
            val sharingState = sharingStatesByExternalId[externalId]
            when {
                sharingState == null -> SharingStateReadyError.NotFound(externalId)
                sharingState.sharingStateType !in typesThatCanBeSetReady -> SharingStateReadyError.InvalidSharingState(externalId, sharingState.sharingStateType)
                else -> null
            }
        }

        return if (errors.isEmpty())
            ParseResult.Success(SharingStateReadyParsed(externalIds.map { sharingStatesByExternalId.getValue(it) }))
        else
            ParseResult.Failure(errors)
    }
}
