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
import org.eclipse.tractusx.bpdm.pool.model.error.SiteContentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteContentParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service

/**
 * Validates the content one site states — its header and the main address it brings of its own.
 */
@Service
class SiteContentParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressContentParser: AddressContentParser
) {

    /**
     * Validates each content and reports either the validated content or every problem found in that entry. [siteBpns]
     * and [mainAddressBpns] are positional with [contents]: null for a create, the site's and its main address's own
     * BPNs for an update, so an update may re-submit its own existing identifiers and has its states judged against the
     * successions both take part in.
     */
    fun parse(
        contents: List<SiteContentRequest>,
        siteBpns: List<String?>,
        mainAddressBpns: List<String?>
    ): List<ParseResult<SiteContentParsed, SiteContentParseError>> {
        val headerResults = siteHeaderParser.parse(contents.map { it.header }, siteBpns)
        val mainAddressResults = addressContentParser.parse(contents.map { it.mainAddress }, mainAddressBpns)

        return zipParseResults(headerResults, mainAddressResults, ::SiteContentParsed)
    }
}
