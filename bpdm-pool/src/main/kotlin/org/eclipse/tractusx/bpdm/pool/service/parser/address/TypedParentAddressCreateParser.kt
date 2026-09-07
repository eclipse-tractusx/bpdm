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
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.springframework.stereotype.Service

/**
 * Validates address-create requests whose parents are already given as an explicit legal-entity BPN and an optional site
 * BPN: the parents, their mutual consistency, and the new address content.
 */
@Service
class TypedParentAddressCreateParser(
    private val addressContentParser: AddressContentParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val siteBpnParser: SiteBpnParser,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator
) {

    /**
     * Validates each request and reports either the validated address with its resolved parents or every problem found in
     * that entry.
     */
    fun parse(requests: List<AddressCreateTypedParentsRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> {
        val contentResults = addressContentParser.parse(requests.map { it.content }, requests.map { null })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val siteResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val consistentSiteResults: List<ParseResult<SiteDb?, AddressCreateParseError>> =
            crossValidateParseResults(legalEntityResults, siteResults) { legalEntity, site ->
                siteLegalEntityConsistencyValidator.check(legalEntity, site)
            }

        return zipParseResults(contentResults, legalEntityResults, consistentSiteResults) { content, legalEntity, site ->
            AddressCreateParsed(legalEntity, site, content)
        }
    }
}
