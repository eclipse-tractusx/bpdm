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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Validates address-update requests: the target address, an optional parent site to assign it to, and the new address
 * content.
 */
@Service
class AddressUpdateParser(
    private val addressContentParser: AddressContentParser,
    private val addressBpnParser: AddressBpnParser,
    private val siteBpnParser: SiteBpnParser,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator,
    private val renderingValidator: ScriptVariantRenderingValidator
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> {
        val contentResults = addressContentParser.parse(requests.map { it.content }, requests.map { it.addressBpn })
        val targetResults = addressBpnParser.parse(requests.map { it.addressBpn })
        val siteResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val consistentSiteResults: List<ParseResult<SiteDb?, AddressUpdateParseError>> =
            crossValidateParseResults(targetResults, siteResults) { target, site ->
                siteLegalEntityConsistencyValidator.check(target.legalEntity, site)
            }
        val renderingContentResults: List<ParseResult<LogisticAddressParsed, AddressUpdateParseError>> =
            crossValidateParseResults(targetResults, contentResults) { target, content ->
                renderingValidator.check(
                    dependentScriptCodesOf(target),
                    content.scriptVariants.map { it.scriptCode.technicalKey }
                )
            }

        return zipParseResults(renderingContentResults, targetResults, consistentSiteResults) { content, target, site ->
            AddressUpdateParsed(target, site, content)
        }
    }

    /**
     * The script codes whose renderings this address owes to the business partners built on it — its legal entity when it
     * is that entity's legal address, and every site it is the main address of.
     */
    private fun dependentScriptCodesOf(address: LogisticAddressDb): List<String> {
        val legalEntityScriptCodes = address.legalEntity
            ?.takeIf { it.legalAddress == address }
            ?.scriptVariants?.map { it.scriptCode.technicalKey }
            .orEmpty()
        val siteScriptCodes = address.sites
            .filter { it.mainAddress == address }
            .flatMap { site -> site.scriptVariants.map { it.scriptCode.technicalKey } }

        return legalEntityScriptCodes.plus(siteScriptCodes).distinct()
    }
}
