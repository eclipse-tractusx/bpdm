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

import org.eclipse.tractusx.bpdm.pool.model.error.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressCreateTypedParentsRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Parses address-create requests whose parents are given as *typed* BPNs into the resolved [AddressCreateParsed] command
 * consumed by [org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreateService]. Validates address content (as a
 * create, so no owner BPN) and resolves the legal-entity / site BPNs to entities — yielding `UnresolvableLegalEntity` /
 * `UnresolvableSite` on a miss — combining the independent verdicts with `zipParseResults`. Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class TypedParentAddressCreateParser(
    private val addressContentParser: AddressContentParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val siteBpnParser: SiteBpnParser
) {

    fun parse(requests: List<AddressCreateTypedParentsRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> {
        val contentResults = addressContentParser.parse(requests.map { it.content }, requests.map { null })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val siteResults = siteBpnParser.parse(requests.map { it.siteBpn })

        return zipParseResults(contentResults, legalEntityResults, siteResults) { content, legalEntity, site ->
            AddressCreateParsed(legalEntity, site, content.address, content.scriptVariants)
        }
    }
}
