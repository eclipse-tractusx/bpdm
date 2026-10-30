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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.v6.exception.BusinessPartnerSharingErrorV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.SharingStateTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.sharingstate.SharingStateV6Dto
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.InvalidSharingState
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.NotFound
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.springframework.stereotype.Component

/**
 * Maps sharing state records and errors to their API v6 form.
 */
@Component
class ApiV6SharingStateMapper {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: SharingStateRecord): SharingStateV6Dto =
        SharingStateV6Dto(
            externalId = record.externalId,
            sharingStateType = toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            sharingProcessStarted = record.sharingProcessStarted,
            taskId = record.taskId
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

    private fun toDto(sharingStateType: SharingStateType): SharingStateTypeV6 =
        when (sharingStateType) {
            SharingStateType.Pending -> SharingStateTypeV6.Pending
            SharingStateType.Success -> SharingStateTypeV6.Success
            SharingStateType.Error -> SharingStateTypeV6.Error
            SharingStateType.Initial -> SharingStateTypeV6.Initial
            SharingStateType.Ready -> SharingStateTypeV6.Ready
        }

    private fun toDto(sharingError: BusinessPartnerSharingErrorCode): BusinessPartnerSharingErrorV6 =
        when (sharingError) {
            BusinessPartnerSharingErrorCode.SharingProcessError -> BusinessPartnerSharingErrorV6.SharingProcessError
            BusinessPartnerSharingErrorCode.SharingTimeout -> BusinessPartnerSharingErrorV6.SharingTimeout
            BusinessPartnerSharingErrorCode.BpnNotInPool -> BusinessPartnerSharingErrorV6.BpnNotInPool
            BusinessPartnerSharingErrorCode.MissingTaskID -> BusinessPartnerSharingErrorV6.MissingTaskID
            BusinessPartnerSharingErrorCode.NaturalPersonError -> BusinessPartnerSharingErrorV6.NaturalPersonError
            BusinessPartnerSharingErrorCode.BpnErrorNotFound -> BusinessPartnerSharingErrorV6.BpnErrorNotFound
            BusinessPartnerSharingErrorCode.BpnErrorTooManyOptions -> BusinessPartnerSharingErrorV6.BpnErrorTooManyOptions
            BusinessPartnerSharingErrorCode.MandatoryFieldValidationFailed -> BusinessPartnerSharingErrorV6.MandatoryFieldValidationFailed
            BusinessPartnerSharingErrorCode.BlacklistCountryPresent -> BusinessPartnerSharingErrorV6.BlacklistCountryPresent
            BusinessPartnerSharingErrorCode.UnknownSpecialCharacters -> BusinessPartnerSharingErrorV6.UnknownSpecialCharacters
        }
}
