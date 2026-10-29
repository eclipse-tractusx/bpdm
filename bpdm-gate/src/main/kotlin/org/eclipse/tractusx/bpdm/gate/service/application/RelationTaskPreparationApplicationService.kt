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

package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskPreparationError
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskPreparationParsed
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskPreparationResult
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationSharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.RelationTaskPreparationParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which relations staged for sharing become golden record tasks.
 */
@Service
class RelationTaskPreparationApplicationService(
    private val sharingStateOperation: RelationSharingStateOperationService,
    private val preparationParser: RelationTaskPreparationParser
) {

    /**
     * Prepares up to [limit] relations staged for sharing: fails those that cannot become a golden record task, leaves
     * those whose endpoints are not yet shared ready for a later run, and returns the candidates for the others.
     */
    @Transactional
    fun prepareStaged(limit: Int): RelationTaskPreparationResult {
        val stagedRelations = sharingStateOperation.findStaged(limit)
        val preparations = preparationParser.parse(stagedRelations).map { result ->
            when (result) {
                is ParseResult.Success -> result.parsed
                is ParseResult.Failure -> toFailed(result.errors)
            }
        }
        sharingStateOperation.prepare(preparations)

        val candidates = preparations.filterIsInstance<RelationTaskPreparationParsed.Sendable>().map { it.candidate }
        return RelationTaskPreparationResult(preparations.size, candidates)
    }

    private fun toFailed(errors: List<RelationTaskPreparationError>) =
        RelationTaskPreparationParsed.Failed(
            relation = errors.first().relation,
            errorCode = RelationSharingStateErrorCode.SharingProcessError,
            errorMessage = errors.joinToString { it.message }
        )
}
