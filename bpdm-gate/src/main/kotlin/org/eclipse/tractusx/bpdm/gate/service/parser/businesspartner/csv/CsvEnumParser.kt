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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.springframework.stereotype.Service

/**
 * Enforces that a CSV cell holding a choice names one of the choice's values exactly.
 */
@Service
class CsvEnumParser {

    /**
     * Returns the value [cell] names according to [valueOf], absent for an empty cell, or fails if [cell] names no
     * value.
     */
    fun <T : Enum<T>> parse(cell: CsvCell, valueOf: (String) -> T): ParseResult<T?, BusinessPartnerCsvRowError> {
        val value = cell.value ?: return ParseResult.Success(null)
        return try {
            ParseResult.Success(valueOf(value))
        } catch (e: IllegalArgumentException) {
            ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.InvalidEnumValue(cell.column))
        }
    }
}
