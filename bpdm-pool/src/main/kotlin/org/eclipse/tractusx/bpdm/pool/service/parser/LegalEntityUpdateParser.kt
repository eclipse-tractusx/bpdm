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

import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Parses legal-entity-update requests into the [LegalEntityUpdateParsed] command consumed by
 * [org.eclipse.tractusx.bpdm.pool.service.operation.LegalEntityUpdateService]. Resolves the target BPN to its existing
 * entity (yielding `UnresolvableLegalEntity` on a miss) and validates the header + identifier-uniqueness + legal-address
 * content — the identifier and legal-address duplicate checks are scoped by the resolved target's own BPN so it may keep
 * its own identifiers. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityUpdateParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val addressContentParser: AddressContentParser
) {

    fun parse(requests: List<LegalEntityUpdateRequest>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>> {
        val targetResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        val headers = requests.map { it.content.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        // An identifier already owned by the target legal entity itself is not a duplicate; its BPN comes from the target.
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.bpn }
        val duplicateErrors = duplicateValidator.validate(headers, ownerBpns)
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        // The legal address's duplicate check is scoped to its owning address; its BPN comes from the resolved target.
        val legalAddressOwnerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.legalAddress?.bpn }
        val legalAddressResults = addressContentParser.parse(requests.map { it.content.legalAddress }, legalAddressOwnerBpns)

        return zipParseResults(mergedHeaderResults, targetResults, legalAddressResults) { header, target, legalAddress ->
            LegalEntityUpdateParsed(target, LegalEntityContentParsed(header, legalAddress))
        }
    }
}
