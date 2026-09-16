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
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service

/**
 * Validates site-create requests that bring a main address of their own: the parent legal entity, the header content
 * and that new address.
 */
@Service
class SiteCreateWithOwnMainAddressParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val addressContentParser: AddressContentParser,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the validated site with its resolved parent or every problem found in
     * that entry.
     */
    fun parse(requests: List<SiteCreateRequest>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> =
        coverageValidator.applyTo(parseWithoutScriptVariantCoverage(requests), ::coverageWrites) { it }

    /**
     * Validates each request as [parse] does, except for script variant coverage, for a caller that writes further
     * addresses and judges coverage over all of them together.
     */
    fun parseWithoutScriptVariantCoverage(
        requests: List<SiteCreateRequest>
    ): List<ParseResult<SiteCreateParsed, SiteCreateEntryParseError>> {
        val contentResults = parseContent(requests.map { it.content })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        return zipParseResults(contentResults, legalEntityResults) { content, legalEntity ->
            SiteCreateParsed(legalEntity, content)
        }
    }

    /**
     * Validates each site's content as a creation, whichever legal entity it turns out to be created under.
     */
    fun parseContent(requests: List<SiteContentRequest>): List<ParseResult<SiteContentParsed, SiteCreateEntryParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val mainAddresses = requests.map { it.mainAddress }
        val mainAddressResults = addressContentParser.parse(mainAddresses, mainAddresses.map { null })

        return zipParseResults(headerResults, mainAddressResults) { header, mainAddress ->
            SiteContentParsed(header, mainAddress)
        }
    }

    // What this write leaves behind, as script variant coverage sees it.
    private fun coverageWrites(parsed: SiteCreateParsed): List<AddressCoverageWrite> =
        listOf(
            AddressCoverageWrite.Created(
                partners = listOf(PartnerScriptCodes(bpn = null, parsed.content.header.scriptCodes())),
                scriptCodes = parsed.content.mainAddress.scriptCodes()
            )
        )
}
