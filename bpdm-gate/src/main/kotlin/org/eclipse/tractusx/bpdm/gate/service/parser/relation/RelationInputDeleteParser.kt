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

package org.eclipse.tractusx.bpdm.gate.service.parser.relation

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputDeleteError
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputDeleteParsed
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputDeleteRequest
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * Resolves the relation input a delete request names.
 */
@Service
class RelationInputDeleteParser(
    private val relationStageRepository: RelationStageRepository
) {

    /**
     * Parses [request] for [tenantBpnl], succeeding with the named relation input if the tenant has it and failing
     * otherwise.
     */
    fun parse(request: RelationInputDeleteRequest, tenantBpnl: String): ParseResult<RelationInputDeleteParsed, RelationInputDeleteError> {
        val input = relationStageRepository.findOne(
            Specification.allOf(
                RelationStageRepository.Specs.byTenantBpnL(tenantBpnl),
                RelationStageRepository.Specs.byStage(StageType.Input),
                RelationStageRepository.Specs.byExternalIds(listOf(request.externalId))
            )
        ).orElse(null)

        return if (input != null)
            ParseResult.Success(RelationInputDeleteParsed(input))
        else
            ParseResult.ofSingleFailure(RelationInputDeleteError.NotFound(request.externalId))
    }
}
