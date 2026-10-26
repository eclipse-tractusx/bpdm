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

package org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.parsed.BusinessPartnerOutputParsedMapper
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.PoolGoldenRecordsRequest
import org.springframework.stereotype.Service

/**
 * Derives the output data business partner outputs take on from golden records.
 */
@Service
class GoldenRecordAlignmentParser(
    private val legalEntityParser: LegalEntityAlignmentParser,
    private val siteParser: SiteAlignmentParser,
    private val addressParser: AddressAlignmentParser,
    private val parsedMapper: BusinessPartnerOutputParsedMapper
) {

    /**
     * Returns the output data each of [outputs] takes on from the golden records in [goldenRecords] it references,
     * keeping what the stored output holds more recently.
     */
    fun parse(outputs: List<BusinessPartnerDb>, goldenRecords: PoolGoldenRecordsRequest): List<ParseResult<BusinessPartnerOutputParsed, GoldenRecordAlignmentError>> =
        outputs.map { parse(it, goldenRecords) }

    private fun parse(output: BusinessPartnerDb, goldenRecords: PoolGoldenRecordsRequest): ParseResult<BusinessPartnerOutputParsed, GoldenRecordAlignmentError> =
        zipParseResults(
            legalEntityParser.parse(output, goldenRecords),
            siteParser.parse(output, goldenRecords),
            addressParser.parse(output, goldenRecords)
        ) { legalEntity, site, address ->
            parsedMapper.toParsed(output, legalEntity, site ?: address?.adoptedSite, address)
        }
}
