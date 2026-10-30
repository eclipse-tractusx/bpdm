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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv7

import org.eclipse.tractusx.bpdm.gate.api.v7.exception.BusinessPartnerSharingError as BusinessPartnerSharingErrorDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.SharingStateType as SharingStateTypeDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.InvalidSharingState
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.NotFound
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.springframework.stereotype.Component

/**
 * Maps sharing state records and errors to their API v7 form.
 */
@Component
class ApiV7SharingStateMapper {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: SharingStateRecord): SharingStateDto =
        SharingStateDto(
            externalId = record.externalId,
            sharingStateType = toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            sharingProcessStarted = record.sharingProcessStarted,
            taskId = record.taskId,
            updatedAt = record.updatedAt
        )

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: SharingStateReadyError): String =
        when (error) {
            is NotFound ->
                "Business partner with external ID '${error.externalId}' not found"
            is InvalidSharingState ->
                "Business partner with external ID '${error.externalId}' is in sharing state ${error.sharingStateType}, " +
                        "but only business partners in an initial or error state can be set ready"
        }

    /**
     * Returns the API sharing state type for [sharingStateType].
     */
    fun toDto(sharingStateType: SharingStateType): SharingStateTypeDto =
        when (sharingStateType) {
            SharingStateType.Pending -> SharingStateTypeDto.Pending
            SharingStateType.Success -> SharingStateTypeDto.Success
            SharingStateType.Error -> SharingStateTypeDto.Error
            SharingStateType.Initial -> SharingStateTypeDto.Initial
            SharingStateType.Ready -> SharingStateTypeDto.Ready
        }

    /**
     * Returns the API sharing error code for [sharingError].
     */
    fun toDto(sharingError: BusinessPartnerSharingErrorCode): BusinessPartnerSharingErrorDto =
        when (sharingError) {
            BusinessPartnerSharingErrorCode.SharingProcessError -> BusinessPartnerSharingErrorDto.SharingProcessError
            BusinessPartnerSharingErrorCode.SharingTimeout -> BusinessPartnerSharingErrorDto.SharingTimeout
            BusinessPartnerSharingErrorCode.BpnNotInPool -> BusinessPartnerSharingErrorDto.BpnNotInPool
            BusinessPartnerSharingErrorCode.MissingTaskID -> BusinessPartnerSharingErrorDto.MissingTaskID
            BusinessPartnerSharingErrorCode.NaturalPersonError -> BusinessPartnerSharingErrorDto.NaturalPersonError
            BusinessPartnerSharingErrorCode.BpnErrorNotFound -> BusinessPartnerSharingErrorDto.BpnErrorNotFound
            BusinessPartnerSharingErrorCode.BpnErrorTooManyOptions -> BusinessPartnerSharingErrorDto.BpnErrorTooManyOptions
            BusinessPartnerSharingErrorCode.MandatoryFieldValidationFailed -> BusinessPartnerSharingErrorDto.MandatoryFieldValidationFailed
            BusinessPartnerSharingErrorCode.BlacklistCountryPresent -> BusinessPartnerSharingErrorDto.BlacklistCountryPresent
            BusinessPartnerSharingErrorCode.UnknownSpecialCharacters -> BusinessPartnerSharingErrorDto.UnknownSpecialCharacters
        }
}
