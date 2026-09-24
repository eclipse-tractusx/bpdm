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
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.util.parsedOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates site-update requests for a site that owns its main address: the target site, the new header content and
 * the new content of that address.
 */
@Service
class SiteUpdateWithOwnMainAddressParser(
    private val siteContentParser: SiteContentParser,
    private val siteBpnParser: SiteBpnParser
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteUpdateRequest>): List<ParseResult<SiteUpdateParsed, SiteUpdateParseError>> {
        val targetResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val targets = targetResults.map { it.parsedOrNull() }
        val contentResults = siteContentParser.parse(
            requests.map { it.content },
            siteBpns = targets.map { it?.bpn },
            mainAddressBpns = targets.map { it?.mainAddress?.bpn }
        )

        return zipParseResults(contentResults, targetResults) { content, target ->
            SiteUpdateParsed(target, content)
        }
    }
}
