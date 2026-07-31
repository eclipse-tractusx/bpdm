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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
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
    fun parse(requests: List<SiteCreateRequest>): List<ParseResult<SiteCreateParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val mainAddresses = requests.map { it.content.mainAddress }
        val mainAddressResults = addressContentParser.parse(mainAddresses, mainAddresses.map { null })
        val coveredHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(mainAddressResults, headerResults) { mainAddress, header ->
                scriptVariantCoverageValidator.check(mainAddress.scriptCodes(), listOf(PartnerScriptCodes(bpn = null, header.scriptCodes())))
            }

        return zipParseResults(coveredHeaderResults, legalEntityResults, mainAddressResults) { header, legalEntity, mainAddress ->
            SiteCreateParsed(legalEntity, SiteContentParsed(header, mainAddress))
        }
    }
}
