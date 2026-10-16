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

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.csv

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvRowError
import org.springframework.stereotype.Service

/**
 * Enforces that a business partner uploaded as CSV states at most the address types an upload may create: a site main
 * address or an additional address.
 */
@Service
class BusinessPartnerCsvAddressTypeParser(
    private val enumParser: CsvEnumParser
) {

    private val supportedAddressTypes = setOf(AddressType.SiteMainAddress, AddressType.AdditionalAddress)

    /**
     * Returns the address type [cell] names, absent for an empty cell, or fails if it names no address type or one an
     * upload may not state.
     */
    fun parse(cell: CsvCell): ParseResult<AddressType?, BusinessPartnerCsvRowError> =
        when (val result = enumParser.parse(cell, AddressType::valueOf)) {
            is ParseResult.Failure -> result
            is ParseResult.Success -> result.parsed
                ?.takeIf { it !in supportedAddressTypes }
                ?.let { ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.UnsupportedAddressType(it)) }
                ?: result
        }
}
