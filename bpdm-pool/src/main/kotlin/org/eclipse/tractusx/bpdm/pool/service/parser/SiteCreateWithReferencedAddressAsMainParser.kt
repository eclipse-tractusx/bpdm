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
 * Validates site-create requests whose main address is an existing address referenced by BPN.
 */
@Service
class SiteCreateWithReferencedAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val addressBpnParser: AddressBpnParser
) {

    /**
     * Validates each request and reports either the validated site with its resolved main address or every problem found
     * in that entry.
     */
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
