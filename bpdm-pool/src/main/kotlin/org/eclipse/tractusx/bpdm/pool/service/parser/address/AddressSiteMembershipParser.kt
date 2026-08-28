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
import org.eclipse.tractusx.bpdm.pool.model.error.AddressSiteMembershipParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSiteMembershipParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSiteMembershipRequest
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates requests stating the complete site membership of an address: every partner must resolve, every stated site
 * must share the address's legal entity, and no site the address is the main address of may be left out.
 *
 * The address's own content is not part of such a request, so no content or script-variant coverage is judged here.
 */
@Service
class AddressSiteMembershipParser(
    private val addressBpnParser: AddressBpnParser,
    private val siteBpnParser: SiteBpnParser,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator,
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator
) {

    /**
     * Validates each request and reports either the resolved address with its stated sites or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<AddressSiteMembershipRequest>): List<ParseResult<AddressSiteMembershipParsed, AddressSiteMembershipParseError>> {
        val addressResults = addressBpnParser.parse(requests.map { it.addressBpn })
        val siteResults = siteBpnParser.parseAllRequired(requests.map { it.siteBpns })
        val consistentSiteResults: List<ParseResult<List<SiteDb>, AddressSiteMembershipParseError>> =
            crossValidateParseResults(addressResults, siteResults) { address, sites ->
                sites.flatMap { siteLegalEntityConsistencyValidator.check(address.legalEntity, it) } +
                        siteMainAddressConsistencyValidator.check(address, sites)
            }

        return zipParseResults(addressResults, consistentSiteResults) { address, sites ->
            AddressSiteMembershipParsed(address, sites)
        }
    }
}
