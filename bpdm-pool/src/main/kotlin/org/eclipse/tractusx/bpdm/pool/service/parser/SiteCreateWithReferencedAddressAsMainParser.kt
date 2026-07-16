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
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Parses "site with referenced address as main" requests into the [SiteCreateWithReferencedAddressAsMainParsed] command
 * consumed by [org.eclipse.tractusx.bpdm.pool.service.operation.SiteCreateWithReferencedAddressAsMainService]. Validates
 * the site header and resolves the referenced address BPN (the re-parent target, yielding `UnresolvableAddress` on a
 * miss). Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class SiteCreateWithReferencedAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser
) {

    fun parse(
        requests: List<SiteCreateWithReferencedAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.content.header })
        val mainAddressTargetResults = addressBpnParser.parse(requests.map { it.mainAddressBpn })

        return zipParseResults(headerResults, mainAddressTargetResults) { header, mainAddress ->
            SiteCreateWithReferencedAddressAsMainParsed(mainAddress, header)
        }
    }
}
