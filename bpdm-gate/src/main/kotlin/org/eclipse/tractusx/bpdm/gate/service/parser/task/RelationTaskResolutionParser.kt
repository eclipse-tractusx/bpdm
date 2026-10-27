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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResultRequest
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides how the relations pending on finished golden record tasks resolve.
 */
@Service
class RelationTaskResolutionParser(
    private val relationRepository: RelationRepository
) {

    /**
     * Resolves each relation still pending on a task in [page] into the outcome of its task, failing those whose task
     * or task result is unusable and leaving out those whose task is still pending.
     */
    @Transactional(readOnly = true)
    fun parse(page: RelationTaskResolutionPageRequest): List<ParseResult<RelationTaskResolutionParsed, RelationTaskResolutionError>> {
        val pendingRelations = relationRepository.findBySharingStateTaskIdIn(page.finishedTasks.taskIds.toSet())
            .filter { it.sharingState?.sharingStateType == RelationSharingStateType.Pending }
        val resultsByTaskId = page.results.associateBy { it.taskId }

        return pendingRelations.mapNotNull { relation ->
            when (val result = resultsByTaskId[checkNotNull(relation.sharingState).taskId]) {
                null -> ParseResult.ofSingleFailure(RelationTaskResolutionError.TaskMissing(relation))
                is RelationTaskResultRequest.Pending -> null
                is RelationTaskResultRequest.Failed ->
                    ParseResult.Success(RelationTaskResolutionParsed.Failed(relation, result.errorCode, result.errorMessage))
                is RelationTaskResultRequest.Succeeded -> parseSucceeded(relation, result)
            }
        }
    }

    private fun parseSucceeded(
        relation: RelationDb,
        result: RelationTaskResultRequest.Succeeded
    ): ParseResult<RelationTaskResolutionParsed, RelationTaskResolutionError> {
        val errors = listOfNotNull(validateEndpoints(relation, result)) + validateValidityPeriods(relation, result)
        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        val output = RelationOutputParsed(
            relation = relation,
            relationType = result.relationType,
            sourceBpn = result.sourceBpn,
            targetBpn = result.targetBpn,
            validityPeriods = result.validityPeriods,
            reasonCode = result.reasonCode
        )
        return ParseResult.Success(RelationTaskResolutionParsed.Succeeded(relation, output))
    }

    private fun validateEndpoints(relation: RelationDb, result: RelationTaskResultRequest.Succeeded): RelationTaskResolutionError? =
        if (result.sourceBpn == result.targetBpn)
            RelationTaskResolutionError.ResultUnusable(relation, "Source and target should not be the same")
        else
            null

    private fun validateValidityPeriods(relation: RelationDb, result: RelationTaskResultRequest.Succeeded): List<RelationTaskResolutionError> =
        result.validityPeriods.mapNotNull { period ->
            period.validTo
                ?.takeIf { !period.validFrom.isBefore(it) }
                ?.let { RelationTaskResolutionError.ResultUnusable(relation, "validFrom '${period.validFrom}' cannot be same or after validTo '$it'.") }
        }
}
