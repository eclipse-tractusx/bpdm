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

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerCsvRowError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.springframework.stereotype.Service

/**
 * Turns the three cells of a business partner state in a CSV row into that state.
 */
@Service
class BusinessPartnerCsvStateParser(
    private val enumParser: CsvEnumParser,
    private val dateTimeParser: CsvDateTimeParser
) {

    /**
     * Returns the state the cells describe, or fails with every cell that holds no valid state type or date-time.
     */
    fun parse(type: CsvCell, validFrom: CsvCell, validTo: CsvCell): ParseResult<BusinessPartnerInputUpsertRaw.State, BusinessPartnerCsvRowError> =
        zipParseResults(
            enumParser.parse(type, BusinessStateType::valueOf),
            dateTimeParser.parse(validFrom),
            dateTimeParser.parse(validTo)
        ) { parsedType, parsedValidFrom, parsedValidTo ->
            BusinessPartnerInputUpsertRaw.State(type = parsedType, validFrom = parsedValidFrom, validTo = parsedValidTo)
        }
}
