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
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateEntryParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderUpdateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates a change to a site's own properties, leaving its main address alone.
 *
 * A site whose main address is the legal address does not own that address, so it states only itself: the address's
 * content is the legal entity's to write.
 */
@Service
class SiteHeaderUpdateParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser
) {

    /**
     * Validates each request and reports either the resolved site with its validated properties or every problem
     * found in that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteHeaderUpdateRequest>): List<ParseResult<SiteHeaderUpdateParsed, SiteUpdateEntryParseError>> {
        val targetResults = siteBpnParser.parse(requests.map { it.siteBpn })
        val headerResults = parseContent(requests.map { it.header })

        return zipParseResults(headerResults, targetResults) { header, target ->
            SiteHeaderUpdateParsed(target, header)
        }
    }

    /**
     * Validates each site's own properties as a change, whichever site they turn out to belong to.
     */
    fun parseContent(requests: List<SiteHeaderRequest>): List<ParseResult<SiteHeaderParsed, SiteUpdateEntryParseError>> =
        siteHeaderParser.parse(requests)
}
