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
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteUpdateRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-update requests: the target site, the new header content, the new main address, and that the main
 * address covers every script code the header names.
 */
@Service
class SiteUpdateParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteUpdateRaw>): List<ParseResult<SiteUpdateParsed, SiteUpdateError>> {
        val targetResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.mainAddress?.bpn }
        val mainAddressResults = addressContentParser.parse(requests.map { it.content.mainAddress }, ownerBpns)

        return zipParseResults(headerResults, targetResults, mainAddressResults) { header, target, mainAddress ->
            SiteUpdateParsed(target, SiteContentParsed(header, mainAddress))
        }.map { result -> result.combine(coverageErrors(result)) { it } }
    }

    // The main address keeps an existing variant its site is still named in, so the variants it carries today cover the
    // header as well as the stated ones.
    private fun coverageErrors(result: ParseResult<SiteUpdateParsed, SiteUpdateError>): List<SiteUpdateError> {
        val parsed = (result as? ParseResult.Success)?.parsed ?: return emptyList()
        val coveredScriptCodes = parsed.content.mainAddress.scriptCodes() + parsed.target.mainAddress.scriptCodes()

        return scriptVariantCoverageValidator.check(coveredScriptCodes, parsed.content.header.scriptCodes())
    }
}
