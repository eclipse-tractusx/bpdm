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
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.parsed.BusinessPartnerOutputParsedMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordsRaw
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
     * Returns the output data of [sharingState] that [base] takes on from the golden records in [goldenRecords] it
     * references, keeping each component [base] holds more recently.
     */
    fun parse(
        sharingState: SharingStateDb,
        storedOutput: BusinessPartnerDb?,
        base: BusinessPartnerOutputContentParsed,
        goldenRecords: GoldenRecordsRaw
    ): ParseResult<BusinessPartnerOutputParsed, GoldenRecordAlignmentError> =
        zipParseResults(
            legalEntityParser.parse(sharingState, base, goldenRecords),
            siteParser.parse(sharingState, base, goldenRecords),
            addressParser.parse(sharingState, base, goldenRecords)
        ) { legalEntity, site, address ->
            parsedMapper.toParsed(sharingState, storedOutput, base, legalEntity, site ?: address?.adoptedSite, address)
        }
}
