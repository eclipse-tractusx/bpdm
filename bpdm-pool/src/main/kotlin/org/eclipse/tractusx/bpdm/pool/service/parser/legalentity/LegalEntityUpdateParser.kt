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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityUpdateEntryParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates legal-entity update requests: the target legal entity, the new header content with its identifier
 * uniqueness, and the new legal address.
 */
@Service
class LegalEntityUpdateParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityContentParser: LegalEntityContentParser,
    private val ownershipValidator: LegalEntityOwnershipValidator,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<LegalEntityUpdateRequest>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>> =
        coverageValidator.applyTo(parseEntries(requests), ::coverageWrites) { it }

    private fun parseEntries(
        requests: List<LegalEntityUpdateRequest>
    ): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateEntryParseError>> {
        val targetResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        val contentWrites = requests.zip(targetResults) { request, targetResult ->
            LegalEntityContentWrite(request.content, (targetResult as? ParseResult.Success)?.parsed)
        }
        val contentResults = legalEntityContentParser.parse(contentWrites)

        val updateResults = zipParseResults(contentResults, targetResults) { content, target ->
            LegalEntityUpdateParsed(target, content)
        }

        val ownershipViolations = ownershipValidator.validate(contentWrites.map { it.headerWrite })

        return updateResults.zip(ownershipViolations) { result, violations -> result.combine(violations) { it } }
    }

    // What this write leaves behind, as script variant coverage sees it.
    private fun coverageWrites(parsed: LegalEntityUpdateParsed): List<AddressCoverageWrite> =
        listOf(
            AddressCoverageWrite.Rewritten(
                address = parsed.target.legalAddress,
                partners = listOf(PartnerScriptCodes(parsed.target.bpn, parsed.content.header.scriptCodes())),
                scriptCodes = parsed.content.legalAddress.scriptCodes()
            )
        )
}
