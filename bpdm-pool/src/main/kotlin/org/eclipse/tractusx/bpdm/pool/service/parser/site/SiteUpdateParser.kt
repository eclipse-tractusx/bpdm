/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateEntryParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-update requests: the target site, the new header content and the new main address.
 */
@Service
class SiteUpdateParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser,
    private val addressContentParser: AddressContentParser,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteUpdateRequest>): List<ParseResult<SiteUpdateParsed, SiteUpdateParseError>> =
        coverageValidator.applyTo(parseWithoutScriptVariantCoverage(requests), ::coverageWrites) { it }

    /**
     * Validates each request as [parse] does, except for script variant coverage, for a caller that writes further
     * addresses and judges coverage over all of them together.
     */
    @Transactional(readOnly = true)
    fun parseWithoutScriptVariantCoverage(
        requests: List<SiteUpdateRequest>
    ): List<ParseResult<SiteUpdateParsed, SiteUpdateEntryParseError>> {
        val targetResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.mainAddress?.bpn }
        val mainAddressResults = addressContentParser.parse(requests.map { it.content.mainAddress }, ownerBpns)

        return zipParseResults(headerResults, targetResults, mainAddressResults) { header, target, mainAddress ->
            SiteUpdateParsed(target, SiteContentParsed(header, mainAddress))
        }
    }

    // What this write leaves behind, as script variant coverage sees it.
    private fun coverageWrites(parsed: SiteUpdateParsed): List<AddressCoverageWrite> =
        listOf(
            AddressCoverageWrite.Rewritten(
                address = parsed.target.mainAddress,
                partners = listOf(PartnerScriptCodes(parsed.target.bpn, parsed.content.header.scriptCodes())),
                scriptCodes = parsed.content.mainAddress.scriptCodes()
            )
        )
}
