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
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.springframework.stereotype.Service

/**
 * Enforces that the coordinate cells of a CSV row are numbers and state at least a longitude and a latitude once any
 * of them is filled.
 */
@Service
class CsvCoordinatesParser(
    private val numberParser: CsvNumberParser
) {

    /**
     * Returns the coordinates the cells hold, absent if all three are empty, or fails with every cell that is not a
     * number or is missing although required.
     */
    fun parse(longitude: CsvCell, latitude: CsvCell, altitude: CsvCell): ParseResult<GeoCoordinateRaw?, BusinessPartnerCsvRowError> {
        if (longitude.value == null && latitude.value == null && altitude.value == null) return ParseResult.Success(null)

        return zipParseResults(parseRequired(longitude), parseRequired(latitude), numberParser.parse(altitude)) { parsedLongitude, parsedLatitude, parsedAltitude ->
            GeoCoordinateRaw(parsedLongitude, parsedLatitude, parsedAltitude)
        }
    }

    private fun parseRequired(cell: CsvCell): ParseResult<Double, BusinessPartnerCsvRowError> =
        when (val result = numberParser.parse(cell)) {
            is ParseResult.Failure -> result
            is ParseResult.Success -> result.parsed
                ?.let { ParseResult.Success(it) }
                ?: ParseResult.ofSingleFailure(BusinessPartnerCsvRowError.InvalidNumber(cell.column))
        }
}
