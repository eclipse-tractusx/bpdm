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
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Validates site-update requests: the target site, the new header content and the new main address.
 */
@Service
class SiteUpdateParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser,
    private val addressContentParser: AddressContentParser
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    fun parse(requests: List<SiteUpdateRequest>): List<ParseResult<SiteUpdateParsed, SiteUpdateParseError>> {
        val targetResults = siteBpnParser.parseRequired(requests.map { it.siteBpn })
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.mainAddress?.bpn }
        val mainAddressResults = addressContentParser.parse(requests.map { it.content.mainAddress }, ownerBpns)

        return zipParseResults(headerResults, targetResults, mainAddressResults) { header, target, mainAddress ->
            SiteUpdateParsed(target, SiteContentParsed(header, mainAddress))
        }
    }
}
