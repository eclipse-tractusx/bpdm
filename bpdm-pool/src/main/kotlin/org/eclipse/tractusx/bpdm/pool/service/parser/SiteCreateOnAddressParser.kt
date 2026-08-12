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
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateOnAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-create requests that adopt an existing address as their main address without stating that address's
 * content, taking the address's legal entity as the new site's parent.
 *
 * It yields the referenced-main-address model: this path is that case with the address left as it stands, so it needs no
 * operation of its own.
 */
@Service
class SiteCreateOnAddressParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the validated site on its resolved main address or every problem found
     * in that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteCreateOnAddressRequest>): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val mainAddressResults = addressBpnParser.parse(requests.map { it.mainAddressBpn })
        val coveredHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(mainAddressResults, headerResults) { mainAddress, header ->
                scriptVariantCoverageValidator.check(mainAddress.scriptCodes(), listOf(PartnerScriptCodes(bpn = null, header.scriptCodes())))
            }

        return zipParseResults(mainAddressResults, coveredHeaderResults) { mainAddress, header ->
            SiteCreateWithReferencedAddressAsMainParsed(mainAddress, header, mainAddressContent = null)
        }
    }
}
