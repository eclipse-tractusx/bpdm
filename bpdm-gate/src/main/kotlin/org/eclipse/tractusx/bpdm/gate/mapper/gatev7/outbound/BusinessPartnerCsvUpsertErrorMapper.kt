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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvUpsertError
import org.springframework.stereotype.Component

/**
 * Maps a rejected business partner CSV upload to the error messages the API reports.
 */
@Component
class BusinessPartnerCsvUpsertErrorMapper(
    private val inputErrorMapper: BusinessPartnerInputUpsertErrorMapper
) {

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: BusinessPartnerCsvUpsertError): String =
        when (error) {
            is BusinessPartnerCsvUpsertError.InvalidTenantBpnl ->
                "The uploading tenant '${error.tenantBpnl.orEmpty()}' is not identified by a legal entity BPN"
            is BusinessPartnerCsvUpsertError.TenantLegalEntityNotFound ->
                "No legal entity with BPN '${error.tenantBpnl}' of the uploading tenant found in the Pool"
            is BusinessPartnerCsvUpsertError.InvalidRow ->
                "Row - ${error.rowNumber}${error.externalId?.let { ", External ID - $it" }.orEmpty()} has error: ${toProblem(error.error)}"
            is BusinessPartnerCsvUpsertError.InputRejected -> inputErrorMapper.toMessage(error.error)
        }

    private fun toProblem(error: BusinessPartnerCsvRowError): String =
        when (error) {
            is BusinessPartnerCsvRowError.MissingExternalId -> "Column 'externalId' is missing and can not be empty or blank."
            is BusinessPartnerCsvRowError.InvalidDate -> "Invalid date format in field '${error.column}'."
            is BusinessPartnerCsvRowError.InvalidEnumValue -> "Invalid enum value in field '${error.column}'."
            is BusinessPartnerCsvRowError.InvalidNumber -> "Invalid or missing number in field '${error.column}'."
            is BusinessPartnerCsvRowError.UnsupportedAddressType ->
                "Invalid address type '${error.addressType}'. Only 'SiteMainAddress' and 'AdditionalAddress' are allowed."
        }
}
