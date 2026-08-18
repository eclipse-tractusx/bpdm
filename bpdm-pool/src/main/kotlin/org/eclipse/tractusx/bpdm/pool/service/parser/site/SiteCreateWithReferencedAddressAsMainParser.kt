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

import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-create requests whose main address is an existing address referenced by BPN, taking the stated address
 * content as the new content of that address — which is therefore what has to cover the site's script codes.
 *
 * It does *not* judge the coverage the referenced address's other business partners need. Only the golden record task
 * path states such a request, and it writes several partners of that address in one transaction, so it decides coverage
 * at task scope. An API endpoint built on this parser would have to add that check itself.
 */
@Service
class SiteCreateWithReferencedAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator,
) {

    /**
     * Validates each request and reports either the validated site with its resolved main address or every problem found
     * in that entry.
     */
    @Transactional(readOnly = true)
    fun parse(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val mainAddressTargetResults = addressBpnParser.parse(requests.map { it.mainAddressBpn })
        val ownerBpns = mainAddressTargetResults.map { (it as? ParseResult.Success)?.parsed?.bpn }
        val mainAddressResults = addressContentParser.parse(requests.map { it.mainAddress }, ownerBpns)

        val coveredHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(mainAddressResults, headerResults) { mainAddress, header ->
                scriptVariantCoverageValidator.check(mainAddress.scriptCodes(), listOf(PartnerScriptCodes(bpn = null, header.scriptCodes())))
            }

        return zipParseResults(coveredHeaderResults, mainAddressTargetResults, mainAddressResults) { header, target, mainAddress ->
            SiteCreateWithReferencedAddressAsMainParsed(target, header, mainAddress)
        }
    }
}
