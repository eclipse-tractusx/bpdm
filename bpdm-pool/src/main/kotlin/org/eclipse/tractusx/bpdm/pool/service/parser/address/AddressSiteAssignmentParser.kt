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
import org.eclipse.tractusx.bpdm.pool.model.error.AddressSiteAssignmentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSiteAssignmentParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSiteAssignmentRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates requests to add an address to a site's membership: both partners must resolve and must share a legal entity.
 *
 * The address's own content is not part of such a request, so no content or script-variant coverage is judged here.
 */
@Service
class AddressSiteAssignmentParser(
    private val addressBpnParser: AddressBpnParser,
    private val siteBpnParser: SiteBpnParser,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator
) {

    /**
     * Validates each request and reports either the resolved address and site or every problem found in that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<AddressSiteAssignmentRequest>): List<ParseResult<AddressSiteAssignmentParsed, AddressSiteAssignmentParseError>> {
        val addressResults = addressBpnParser.parse(requests.map { it.addressBpn })
        val siteResults = siteBpnParser.parseRequired(requests.map { it.siteBpn })
        val consistentSiteResults: List<ParseResult<SiteDb, AddressSiteAssignmentParseError>> =
            crossValidateParseResults(addressResults, siteResults) { address, site ->
                siteLegalEntityConsistencyValidator.check(address.legalEntity, site)
            }

        return zipParseResults(addressResults, consistentSiteResults) { address, site ->
            AddressSiteAssignmentParsed(address, site)
        }
    }
}
