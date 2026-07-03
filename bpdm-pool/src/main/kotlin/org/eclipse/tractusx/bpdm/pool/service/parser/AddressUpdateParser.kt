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

import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Parses address-update requests into the resolved [AddressUpdateParsed] command consumed by
 * [org.eclipse.tractusx.bpdm.pool.service.operation.AddressUpdateService]. Resolves the target BPN to its existing entity
 * (yielding `UnresolvableAddress` on a miss), resolves the optional site parent, and validates address content as an
 * update — the target's own BPN is passed as the identifier-uniqueness owner so it may keep its own identifiers.
 * Verdicts are combined with `zipParseResults`. Order-preserving positional contract (see
 * [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class AddressUpdateParser(
    private val addressContentParser: AddressContentParser,
    private val addressBpnParser: AddressBpnParser,
    private val siteBpnParser: SiteBpnParser
) {

    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> {
        val contentResults = addressContentParser.parse(requests.map { it.content }, requests.map { it.addressBpn })
        val targetResults = addressBpnParser.parse(requests.map { it.addressBpn })
        val siteResults = siteBpnParser.parse(requests.map { it.siteBpn })

        return zipParseResults(contentResults, targetResults, siteResults) { content, target, site ->
            AddressUpdateParsed(target, site, content.address, content.scriptVariants)
        }
    }
}
