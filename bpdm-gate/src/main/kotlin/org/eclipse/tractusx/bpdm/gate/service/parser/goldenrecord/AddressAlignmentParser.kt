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
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressAlignmentParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressGoldenRecordRequest
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.PoolGoldenRecordsRequest
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.AdditionalSite
import org.springframework.stereotype.Service

/**
 * Decides how a business partner output takes on the golden record of its address, including the site an address
 * newly belongs to.
 */
@Service
class AddressAlignmentParser(
    private val siteParser: SiteAlignmentParser
) {

    /**
     * Returns how the address of [output] among [goldenRecords] aligns the output, or null when it was not requested or
     * is older than the stored one.
     */
    fun parse(output: BusinessPartnerDb, goldenRecords: PoolGoldenRecordsRequest): ParseResult<AddressAlignmentParsed?, GoldenRecordAlignmentError> {
        val bpn = output.bpnA?.takeIf { it in goldenRecords.requested.addresses } ?: return ParseResult.Success(null)
        val address = goldenRecords.addresses[bpn]
            ?: return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.GoldenRecordMissing(output, bpn))
        val isOutdated = output.addressUpdatedAt?.let { address.updatedAt.isBefore(it) } == true
        if (isOutdated) return ParseResult.Success(null)

        val siteBpnToAdopt = address.siteBpn?.takeIf { output.bpnS == null }
        val adoptedSite = siteBpnToAdopt?.let { goldenRecords.sites[it] }
        val addressType = address.addressType ?: output.postalAddress.addressType
        if (addressType == null || (siteBpnToAdopt != null && adoptedSite == null))
            return ParseResult.Failure(
                listOfNotNull(
                    siteBpnToAdopt?.takeIf { adoptedSite == null }?.let { GoldenRecordAlignmentError.AdoptedSiteMissing(output, it) },
                    GoldenRecordAlignmentError.AddressTypeMissing(output, bpn).takeIf { addressType == null }
                )
            )

        return ParseResult.Success(
            AddressAlignmentParsed(
                address = address.copy(scriptVariants = address.scriptVariants.distinctBy { it.scriptCode }),
                addressType = addressType,
                additionalSites = additionalSitesOf(address, output.bpnS ?: siteBpnToAdopt, goldenRecords),
                adoptedSite = adoptedSite?.let(siteParser::normalize)
            )
        )
    }

    // Which of the address's sites is additional is relative to the site this output tracks, which may be any member,
    // not to the Pool's `bpnSite`, which is only its oldest member.
    private fun additionalSitesOf(address: AddressGoldenRecordRequest, trackedSiteBpn: String?, goldenRecords: PoolGoldenRecordsRequest): List<AdditionalSite> =
        (listOfNotNull(address.siteBpn) + address.additionalSiteBpns)
            .distinct()
            .filterNot { it == trackedSiteBpn }
            .map { AdditionalSite(it, goldenRecords.sites[it]?.name) }
}
