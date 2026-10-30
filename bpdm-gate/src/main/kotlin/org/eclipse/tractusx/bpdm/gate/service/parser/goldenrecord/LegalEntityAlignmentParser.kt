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
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordsRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRaw
import org.springframework.stereotype.Service

/**
 * Decides which legal entity golden record a business partner output takes on.
 */
@Service
class LegalEntityAlignmentParser {

    /**
     * Returns the golden record of the legal entity of [base] among [goldenRecords], or null when it was not requested
     * or is older than the one [base] holds.
     */
    fun parse(
        sharingState: SharingStateDb,
        base: BusinessPartnerOutputContentParsed,
        goldenRecords: GoldenRecordsRaw
    ): ParseResult<LegalEntityGoldenRecordRaw?, GoldenRecordAlignmentError> {
        val bpn = base.legalEntityBpn.takeIf { it in goldenRecords.requested.legalEntities } ?: return ParseResult.Success(null)
        val legalEntity = goldenRecords.legalEntities[bpn]
            ?: return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.GoldenRecordMissing(sharingState, bpn))
        val isOutdated = base.legalEntityUpdatedAt?.let { legalEntity.updatedAt.isBefore(it) } == true

        return ParseResult.Success(
            legalEntity.takeUnless { isOutdated }?.copy(scriptVariants = legalEntity.scriptVariants.distinctBy { it.scriptCode })
        )
    }
}
