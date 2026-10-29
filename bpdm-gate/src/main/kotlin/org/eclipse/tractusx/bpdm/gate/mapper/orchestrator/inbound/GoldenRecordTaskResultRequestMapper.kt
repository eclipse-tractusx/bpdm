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

import org.eclipse.tractusx.bpdm.gate.model.sharingstate.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordBpns
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResultRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.ResultState
import org.eclipse.tractusx.orchestrator.api.model.TaskClientStateDto
import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.springframework.stereotype.Component

/**
 * Maps the golden record task states the Orchestrator reports onto the task results the Gate resolves.
 */
@Component
class GoldenRecordTaskResultRequestMapper(
    private val outputMapper: GoldenRecordTaskOutputRequestMapper
) {

    /**
     * Maps [task] onto its result, translating the error type the task failed with first into a sharing error code.
     */
    fun toRequest(task: TaskClientStateDto): GoldenRecordTaskResultRequest =
        when (task.processingState.resultState) {
            ResultState.Pending -> GoldenRecordTaskResultRequest.Pending(task.taskId)
            ResultState.Success -> GoldenRecordTaskResultRequest.Succeeded(task.taskId, outputMapper.toRequest(task.businessPartnerResult), toBpns(task.businessPartnerResult))
            ResultState.Error -> toFailed(task)
        }

    private fun toBpns(businessPartner: BusinessPartner): GoldenRecordBpns =
        with(businessPartner) {
            GoldenRecordBpns(
                legalEntities = setOfNotNull(legalEntity.bpnReference.referenceValue),
                sites = setOfNotNull(site?.bpnReference?.referenceValue),
                addresses = setOfNotNull(
                    legalEntity.legalAddress.bpnReference.referenceValue,
                    site?.siteMainAddress?.bpnReference?.referenceValue,
                    additionalAddress?.bpnReference?.referenceValue
                )
            )
        }

    private fun toFailed(task: TaskClientStateDto): GoldenRecordTaskResultRequest.Failed {
        val errors = task.processingState.errors
        return GoldenRecordTaskResultRequest.Failed(
            taskId = task.taskId,
            errorCode = toErrorCode(errors.firstOrNull()?.type),
            errorMessage = errors.joinToString(" // ") { it.description }.take(255).ifEmpty { null }
        )
    }

    private fun toErrorCode(errorType: TaskErrorType?): BusinessPartnerSharingError =
        when (errorType) {
            TaskErrorType.Timeout -> BusinessPartnerSharingError.SharingTimeout
            TaskErrorType.NaturalPersonError -> BusinessPartnerSharingError.NaturalPersonError
            TaskErrorType.BpnErrorNotFound -> BusinessPartnerSharingError.BpnErrorNotFound
            TaskErrorType.BpnErrorTooManyOptions -> BusinessPartnerSharingError.BpnErrorTooManyOptions
            TaskErrorType.MandatoryFieldValidationFailed -> BusinessPartnerSharingError.MandatoryFieldValidationFailed
            TaskErrorType.BlacklistCountryPresent -> BusinessPartnerSharingError.BlacklistCountryPresent
            TaskErrorType.UnknownSpecialCharacters -> BusinessPartnerSharingError.UnknownSpecialCharacters
            else -> BusinessPartnerSharingError.SharingProcessError
        }
}
