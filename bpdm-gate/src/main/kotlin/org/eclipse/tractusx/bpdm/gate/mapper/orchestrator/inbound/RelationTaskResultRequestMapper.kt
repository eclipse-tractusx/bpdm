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

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResultRequest
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.ResultState
import org.eclipse.tractusx.orchestrator.api.model.TaskClientRelationsStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsErrorType
import org.springframework.stereotype.Component

/**
 * Maps the relation task states the Orchestrator reports onto the task results the Gate resolves.
 */
@Component
class RelationTaskResultRequestMapper {

    /**
     * Maps [task] onto its result, translating the error type the task failed with first into a sharing error code.
     */
    fun toRequest(task: TaskClientRelationsStateDto): RelationTaskResultRequest =
        when (task.processingState.resultState) {
            ResultState.Pending -> RelationTaskResultRequest.Pending(task.taskId)
            ResultState.Success -> toSucceeded(task)
            ResultState.Error -> toFailed(task)
        }

    private fun toSucceeded(task: TaskClientRelationsStateDto): RelationTaskResultRequest.Succeeded =
        with(task.businessPartnerRelationsResult) {
            RelationTaskResultRequest.Succeeded(
                taskId = task.taskId,
                relationType = toRelationType(relationType),
                sourceBpn = businessPartnerSourceBpn,
                targetBpn = businessPartnerTargetBpn,
                validityPeriods = validityPeriods.map { RelationValidityPeriodRaw(validFrom = it.validFrom, validTo = it.validTo) },
                reasonCode = reasonCode
            )
        }

    private fun toFailed(task: TaskClientRelationsStateDto): RelationTaskResultRequest.Failed {
        val errors = task.processingState.errors
        return RelationTaskResultRequest.Failed(
            taskId = task.taskId,
            errorCode = toErrorCode(errors.firstOrNull()?.type),
            errorMessage = errors.joinToString { it.description }.take(250)
        )
    }

    private fun toRelationType(relationType: RelationType): SharableRelationType =
        when (relationType) {
            RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
            RelationType.IsManagedBy -> SharableRelationType.IsManagedBy
            RelationType.IsOwnedBy -> SharableRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> SharableRelationType.IsReplacedBy
        }

    private fun toErrorCode(errorType: TaskRelationsErrorType?): RelationSharingStateErrorCode =
        when (errorType) {
            TaskRelationsErrorType.Timeout -> RelationSharingStateErrorCode.SharingTimeout
            TaskRelationsErrorType.Unspecified, null -> RelationSharingStateErrorCode.SharingProcessError
        }
}
