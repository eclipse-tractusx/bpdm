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

package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates address-update requests: the target address, an optionally stated complete site membership, and the new
 * address content.
 */
@Service
class AddressUpdateParser(
    private val addressContentParser: AddressContentParser,
    private val addressBpnParser: AddressBpnParser,
    private val siteBpnParser: SiteBpnParser,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator,
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator,
    private val partnerReader: AddressPartnerScriptCodeReader
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> {
        val contentResults = addressContentParser.parse(requests.map { it.content }, requests.map { it.addressBpn })
        val targetResults = addressBpnParser.parse(requests.map { it.addressBpn })
        val siteResults = siteBpnParser.parseAll(requests.map { it.siteBpns })
        val consistentSiteResults: List<ParseResult<List<SiteDb>?, AddressUpdateParseError>> =
            crossValidateParseResults(targetResults, siteResults) { target, sites ->
                when (sites) {
                    // A request that states no membership asks for none of it to be judged.
                    null -> emptyList()
                    else -> sites.flatMap { siteLegalEntityConsistencyValidator.check(target.legalEntity, it) } +
                            siteMainAddressConsistencyValidator.check(target, sites)
                }
            }
        val coveredContentResults: List<ParseResult<LogisticAddressParsed, AddressUpdateParseError>> =
            crossValidateParseResults(targetResults, contentResults) { target, content ->
                scriptVariantCoverageValidator.check(content.scriptCodes(), partnerReader.storedPartners(target))
            }

        return zipParseResults(coveredContentResults, targetResults, consistentSiteResults) { content, target, sites ->
            AddressUpdateParsed(target, sites, content)
        }
    }
}
