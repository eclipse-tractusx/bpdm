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
import org.eclipse.tractusx.bpdm.common.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateRaw
import org.eclipse.tractusx.bpdm.pool.model.request.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service

/**
 * Validates site-create requests: the parent legal entity, the header content, the new main address, and that the main
 * address covers every script code the header names.
 */
@Service
class SiteCreateParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the validated site with its resolved parent or every problem found in
     * that entry.
     */
    fun parse(requests: List<SiteCreateRaw>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> {
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val contentResults = parseContent(requests.map { it.content })

        return zipParseResults(contentResults, legalEntityResults) { content, legalEntity -> SiteCreateParsed(legalEntity, content) }
    }

    /**
     * Validates the content of each new site apart from its parent and reports either the validated content or every
     * problem found in that entry; [mainAddressBpns] names the main address an entry states new content for, or null for
     * a main address that does not exist yet.
     */
    fun parseContent(
        contents: List<SiteContentRequest>,
        mainAddressBpns: List<String?> = contents.map { null }
    ): List<ParseResult<SiteContentParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(contents.map { it.header })
        val mainAddressResults = addressContentParser.parse(contents.map { it.mainAddress }, mainAddressBpns)
        val coveredHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(mainAddressResults, headerResults) { mainAddress, header ->
                scriptVariantCoverageValidator.check(mainAddress.scriptCodes(), header.scriptCodes())
            }

        return zipParseResults(coveredHeaderResults, mainAddressResults) { header, mainAddress -> SiteContentParsed(header, mainAddress) }
    }
}
