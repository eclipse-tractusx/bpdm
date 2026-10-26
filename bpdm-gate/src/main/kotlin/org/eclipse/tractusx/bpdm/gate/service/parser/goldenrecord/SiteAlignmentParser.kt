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
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.PoolGoldenRecordsRequest
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SiteGoldenRecordRequest
import org.springframework.stereotype.Service

/**
 * Decides which site golden record a business partner output takes on.
 */
@Service
class SiteAlignmentParser {

    /**
     * Returns the golden record of the site [output] tracks among [goldenRecords], or null when it was not requested or
     * is older than the stored one.
     */
    fun parse(output: BusinessPartnerDb, goldenRecords: PoolGoldenRecordsRequest): ParseResult<SiteGoldenRecordRequest?, GoldenRecordAlignmentError> {
        val bpn = output.bpnS?.takeIf { it in goldenRecords.requested.sites } ?: return ParseResult.Success(null)
        val site = goldenRecords.sites[bpn]
            ?: return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.GoldenRecordMissing(output, bpn))
        val isOutdated = output.siteUpdatedAt?.let { site.updatedAt.isBefore(it) } == true

        return ParseResult.Success(site.takeUnless { isOutdated }?.let(::normalize))
    }

    /**
     * Returns [site] with one script variant per script code.
     */
    fun normalize(site: SiteGoldenRecordRequest): SiteGoldenRecordRequest =
        site.copy(scriptVariants = site.scriptVariants.distinctBy { it.scriptCode })
}
