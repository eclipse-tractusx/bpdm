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

package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.AddressType

/**
 * Reason a business partner CSV upload is rejected: its tenant, one of its rows, or the upsert content it carries.
 */
sealed interface BusinessPartnerCsvUpsertError {
    data class InvalidTenantBpnl(val tenantBpnl: String?) : BusinessPartnerCsvUpsertError
    data class TenantLegalEntityNotFound(val tenantBpnl: String) : BusinessPartnerCsvUpsertError
    data class InvalidRow(val rowNumber: Int, val externalId: String?, val error: BusinessPartnerCsvRowError) : BusinessPartnerCsvUpsertError
    data class InputRejected(val error: BusinessPartnerInputUpsertError) : BusinessPartnerCsvUpsertError
}

/**
 * Reason one row of a business partner CSV upload is invalid, independent of where the row is in the file.
 */
sealed interface BusinessPartnerCsvRowError {
    data object MissingExternalId : BusinessPartnerCsvRowError
    data class InvalidDate(val column: String) : BusinessPartnerCsvRowError
    data class InvalidEnumValue(val column: String) : BusinessPartnerCsvRowError
    data class InvalidNumber(val column: String) : BusinessPartnerCsvRowError
    data class UnsupportedAddressType(val addressType: AddressType) : BusinessPartnerCsvRowError
}
