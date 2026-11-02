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

import org.eclipse.tractusx.bpdm.gate.api.v7.exception.BusinessPartnerSharingErrorV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateV7Dto
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
    fun toDto(record: SharingStateRecord): SharingStateV7Dto =
        SharingStateV7Dto(
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
    fun toDto(sharingStateType: SharingStateType): SharingStateTypeV7 =
        when (sharingStateType) {
            SharingStateType.Pending -> SharingStateTypeV7.Pending
            SharingStateType.Success -> SharingStateTypeV7.Success
            SharingStateType.Error -> SharingStateTypeV7.Error
            SharingStateType.Initial -> SharingStateTypeV7.Initial
            SharingStateType.Ready -> SharingStateTypeV7.Ready
        }

    /**
     * Returns the API sharing error code for [sharingError].
     */
    fun toDto(sharingError: BusinessPartnerSharingErrorCode): BusinessPartnerSharingErrorV7 =
        when (sharingError) {
            BusinessPartnerSharingErrorCode.SharingProcessError -> BusinessPartnerSharingErrorV7.SharingProcessError
            BusinessPartnerSharingErrorCode.SharingTimeout -> BusinessPartnerSharingErrorV7.SharingTimeout
            BusinessPartnerSharingErrorCode.BpnNotInPool -> BusinessPartnerSharingErrorV7.BpnNotInPool
            BusinessPartnerSharingErrorCode.MissingTaskID -> BusinessPartnerSharingErrorV7.MissingTaskID
            BusinessPartnerSharingErrorCode.NaturalPersonError -> BusinessPartnerSharingErrorV7.NaturalPersonError
            BusinessPartnerSharingErrorCode.BpnErrorNotFound -> BusinessPartnerSharingErrorV7.BpnErrorNotFound
            BusinessPartnerSharingErrorCode.BpnErrorTooManyOptions -> BusinessPartnerSharingErrorV7.BpnErrorTooManyOptions
            BusinessPartnerSharingErrorCode.MandatoryFieldValidationFailed -> BusinessPartnerSharingErrorV7.MandatoryFieldValidationFailed
            BusinessPartnerSharingErrorCode.BlacklistCountryPresent -> BusinessPartnerSharingErrorV7.BlacklistCountryPresent
            BusinessPartnerSharingErrorCode.UnknownSpecialCharacters -> BusinessPartnerSharingErrorV7.UnknownSpecialCharacters
        }
}
