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
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithLegalAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * The "legal address as main" path is just the referenced-address case where the referenced main address is the parent
 * legal entity's own legal address, so it produces a [SiteCreateWithReferencedAddressAsMainParsed] and needs no dedicated
 * legal-address operation.
 */
@Service
class SiteCreateWithLegalAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    fun parse(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        return zipParseResults(legalEntityResults, headerResults) { legalEntity, header ->
            // The legal address is the site's main address; its own back-reference gives the parent legal entity, so the
            // referenced-address operation derives the same parent this path used to pass explicitly.
            SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, header)
        }
    }
}
