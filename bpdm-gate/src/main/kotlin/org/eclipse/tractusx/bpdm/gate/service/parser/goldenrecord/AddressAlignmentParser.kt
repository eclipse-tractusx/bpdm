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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.AddressAlignmentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordsRaw
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
     * Returns how the address of [base] among [goldenRecords] aligns the output, or null when it was not requested or
     * is older than the one [base] holds.
     */
    fun parse(
        sharingState: SharingStateDb,
        base: BusinessPartnerOutputContentParsed,
        goldenRecords: GoldenRecordsRaw
    ): ParseResult<AddressAlignmentParsed?, GoldenRecordAlignmentError> {
        val bpn = base.addressBpn.takeIf { it in goldenRecords.requested.addresses } ?: return ParseResult.Success(null)
        val address = goldenRecords.addresses[bpn]
            ?: return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.GoldenRecordMissing(sharingState, bpn))
        val isOutdated = base.addressUpdatedAt?.let { address.updatedAt.isBefore(it) } == true
        if (isOutdated) return ParseResult.Success(null)

        val siteBpnToAdopt = address.siteBpn?.takeIf { base.siteBpn == null }
        val adoptedSite = siteBpnToAdopt?.let { goldenRecords.sites[it] }
        if (siteBpnToAdopt != null && adoptedSite == null)
            return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.AdoptedSiteMissing(sharingState, siteBpnToAdopt))

        val trackedSiteBpn = base.siteBpn ?: siteBpnToAdopt
        val trackedSiteIsMember = trackedSiteBpn != null &&
                (trackedSiteBpn == address.siteBpn || trackedSiteBpn in address.additionalSiteBpns)
        val trackedSite = trackedSiteBpn?.let { goldenRecords.sites[it] }
        if (trackedSiteBpn != null && trackedSiteIsMember && trackedSite == null)
            return ParseResult.ofSingleFailure(GoldenRecordAlignmentError.GoldenRecordMissing(sharingState, trackedSiteBpn))

        val poolType = address.addressType ?: base.addressType
        val isLegalAddress = poolType == AddressType.LegalAddress || poolType == AddressType.LegalAndSiteMainAddress
        // The Pool's site-main role is global; the output's role belongs only to its tracked site.
        val isSiteMainAddress = trackedSiteIsMember && trackedSite?.mainAddressBpn == address.bpn
        val addressType = when {
            isLegalAddress && isSiteMainAddress -> AddressType.LegalAndSiteMainAddress
            isLegalAddress -> AddressType.LegalAddress
            isSiteMainAddress -> AddressType.SiteMainAddress
            else -> AddressType.AdditionalAddress
        }

        return ParseResult.Success(
            AddressAlignmentParsed(
                address = address.copy(scriptVariants = address.scriptVariants.distinctBy { it.scriptCode }),
                addressType = addressType,
                additionalSites = additionalSitesOf(address, trackedSiteBpn, goldenRecords),
                adoptedSite = adoptedSite?.let(siteParser::normalize)
            )
        )
    }

    // Which of the address's sites is additional is relative to the site this output tracks, which may be any member,
    // not to the Pool's `bpnSite`, which is only its oldest member.
    private fun additionalSitesOf(address: AddressGoldenRecordRaw, trackedSiteBpn: String?, goldenRecords: GoldenRecordsRaw): List<AdditionalSiteRaw> =
        (listOfNotNull(address.siteBpn) + address.additionalSiteBpns)
            .distinct()
            .filterNot { it == trackedSiteBpn }
            .map { AdditionalSiteRaw(it, goldenRecords.sites[it]?.name) }
}
