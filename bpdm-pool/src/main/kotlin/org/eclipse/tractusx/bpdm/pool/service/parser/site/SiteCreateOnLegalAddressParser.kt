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
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateEntryParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithLegalAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service

/**
 * Validates site-create requests that take the parent legal entity's legal address as the site main address.
 *
 * It yields the referenced-main-address model: this path is that case with the reference fixed to the parent's legal
 * address, so it needs no operation of its own.
 */
@Service
class SiteCreateOnLegalAddressParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the validated site on its parent's legal address or every problem found
     * in that entry.
     */
    fun parse(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> =
        coverageValidator.applyTo(parseWithoutScriptVariantCoverage(requests), ::coverageWrites) { it }

    /**
     * Validates each request as [parse] does, except for script variant coverage, for a caller that writes further
     * addresses and judges coverage over all of them together.
     */
    fun parseWithoutScriptVariantCoverage(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateEntryParseError>> {
        val headerResults = parseContent(requests.map { it.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        return zipParseResults(legalEntityResults, headerResults) { legalEntity, header ->
            SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, header, mainAddressContent = null)
        }
    }

    /**
     * Validates each site's own properties as a creation, whichever legal address it turns out to be created on.
     */
    fun parseContent(requests: List<SiteHeaderRequest>): List<ParseResult<SiteHeaderParsed, SiteCreateEntryParseError>> =
        siteHeaderParser.parse(requests)

    // What this write leaves behind, as script variant coverage sees it.
    private fun coverageWrites(parsed: SiteCreateWithReferencedAddressAsMainParsed): List<AddressCoverageWrite> =
        listOf(
            AddressCoverageWrite.PartnerOnly(
                address = parsed.mainAddress,
                partners = listOf(PartnerScriptCodes(bpn = null, parsed.siteHeader.scriptCodes()))
            )
        )
}
