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
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.SiteMainAddressNotLegalAddress
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateOnLegalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateOnLegalAddressRequest
import org.eclipse.tractusx.bpdm.pool.util.parsedOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates a change to a site that sits on its legal entity's legal address, covering the site's own properties only.
 *
 * Such a site does not own the address it sits on, so it states only itself: the address's content is the legal
 * entity's to write. A site that owns a main address of its own is therefore not a target of this operation.
 */
@Service
class SiteUpdateOnLegalAddressParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val siteBpnParser: SiteBpnParser
) {

    /**
     * Validates each request and reports either the resolved site with its validated properties or every problem
     * found in that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<SiteUpdateOnLegalAddressRequest>): List<ParseResult<SiteUpdateOnLegalAddressParsed, SiteUpdateParseError>> {
        val targetResults = siteBpnParser.parse(requests.map { it.siteBpn }).map(::requireSittingOnLegalAddress)
        val headerResults = siteHeaderParser.parse(requests.map { it.header }, targetResults.map { it.parsedOrNull()?.bpn })

        return zipParseResults(headerResults, targetResults) { header, target ->
            SiteUpdateOnLegalAddressParsed(target, header)
        }
    }

    private fun requireSittingOnLegalAddress(
        result: ParseResult<SiteDb, SiteUpdateParseError>
    ): ParseResult<SiteDb, SiteUpdateParseError> =
        when (result) {
            is ParseResult.Failure -> result
            is ParseResult.Success -> {
                val site = result.parsed
                if (site.sitsOnLegalAddress()) result
                else ParseResult.ofSingleFailure(SiteMainAddressNotLegalAddress(site.bpn, site.mainAddress.bpn))
            }
        }
}
