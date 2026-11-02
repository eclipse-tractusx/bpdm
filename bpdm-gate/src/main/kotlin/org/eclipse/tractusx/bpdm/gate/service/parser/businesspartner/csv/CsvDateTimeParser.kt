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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Enforces that a CSV cell holding a date and time is written as an ISO date-time.
 */
@Service
class CsvDateTimeParser {

    /**
     * Returns the date-time in [cell], absent for an empty cell, or fails if the cell is not an ISO date-time.
     */
    fun parse(cell: CsvCell): ParseResult<LocalDateTime?, BusinessPartnerCsvRowError> {
        val value = cell.value ?: return ParseResult.Success(null)
        return try {
            ParseResult.Success(LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME))
        } catch (e: DateTimeParseException) {
            ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.InvalidDate(cell.column))
        }
    }
}
