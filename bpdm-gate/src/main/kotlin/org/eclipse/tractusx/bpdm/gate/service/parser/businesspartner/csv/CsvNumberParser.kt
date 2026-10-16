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
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvRowError
import org.springframework.stereotype.Service

/**
 * Enforces that a CSV cell holding a number is a decimal number.
 */
@Service
class CsvNumberParser {

    /**
     * Returns the number in [cell], absent for an empty cell, or fails if the cell is not a number.
     */
    fun parse(cell: CsvCell): ParseResult<Double?, BusinessPartnerCsvRowError> {
        val value = cell.value ?: return ParseResult.Success(null)
        return value.toDoubleOrNull()
            ?.let { ParseResult.Success(it) }
            ?: ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.InvalidNumber(cell.column))
    }
}
