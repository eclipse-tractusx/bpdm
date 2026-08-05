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
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates legal-entity update requests: the target legal entity, the new header content with its identifier
 * uniqueness, the new legal address, and that the new legal address still covers every script code the header and the
 * address's other partners name.
 */
@Service
class LegalEntityUpdateParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val ultimateOwnerUniquenessValidator: UltimateOwnerUniquenessValidator,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator,
    private val partnerReader: AddressPartnerScriptCodeReader
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<LegalEntityUpdateRequest>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>> =
        parseWithoutCoverageCheck(requests).map { result -> result.combine(scriptCodeCoverageErrors(result)) { it } }

    /**
     * Validates each request without judging script variant coverage — for a caller that rewrites several partners of
     * the legal address in one operation set and therefore decides coverage at its own scope.
     */
    @Transactional(readOnly = true)
    fun parseWithoutCoverageCheck(requests: List<LegalEntityUpdateRequest>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>> {
        val targetResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })

        val headers = requests.map { it.content.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        val ownerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.bpn }
        val duplicateErrors = duplicateValidator.validate(headers, ownerBpns)
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        val legalAddressOwnerBpns = targetResults.map { (it as? ParseResult.Success)?.parsed?.legalAddress?.bpn }
        val legalAddressResults = addressContentParser.parse(requests.map { it.content.legalAddress }, legalAddressOwnerBpns)

        val updateResults = zipParseResults(mergedHeaderResults, targetResults, legalAddressResults) { header, target, legalAddress ->
            LegalEntityUpdateParsed(target, LegalEntityContentParsed(header, legalAddress))
        }

        // The ultimate-owner rule spans the whole batch and both the requested flag and the resolved target, so it is
        // folded in at this level rather than into the header result.
        val resolvedTargets = targetResults.map { (it as? ParseResult.Success)?.parsed }
        val ownershipViolations = ultimateOwnerUniquenessValidator.validate(resolvedTargets, headers.map { it.ownershipUltimate })

        return updateResults.zip(ownershipViolations) { result, violations -> result.combine(violations) { it } }
    }

    private fun scriptCodeCoverageErrors(result: ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateParseError>): List<LegalEntityUpdateParseError> {
        val parsed = (result as? ParseResult.Success)?.parsed ?: return emptyList()
        val otherPartners = partnerReader.storedPartners(parsed.target.legalAddress, rewrittenBpns = setOf(parsed.target.bpn))

        return scriptVariantCoverageValidator.check(
            parsed.content.legalAddress.scriptCodes(),
            listOf(PartnerScriptCodes(bpn = null, parsed.content.header.scriptCodes())).plus(otherPartners)
        )
    }
}
