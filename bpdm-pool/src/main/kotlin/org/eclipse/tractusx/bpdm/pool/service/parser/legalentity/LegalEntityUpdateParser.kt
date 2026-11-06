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
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityUpdateRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AlternativeHeadquarterValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates legal-entity update requests: the target legal entity, the new header content with its identifier
 * uniqueness, the new legal address, and that the legal address covers every script code the header names.
 */
@Service
class LegalEntityUpdateParser(
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val ultimateOwnerUniquenessValidator: UltimateOwnerUniquenessValidator,
    private val alternativeHeadquarterValidator: AlternativeHeadquarterValidator,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the resolved target with its validated content or every problem found in
     * that entry.
     */
    @Transactional(readOnly = true)
    fun parse(requests: List<LegalEntityUpdateRaw>): List<ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateError>> {
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
        val alternativeViolations = alternativeHeadquarterValidator.validate(resolvedTargets, headers.map { it.ownershipUltimate })

        return updateResults
            .zip(ownershipViolations) { result, violations -> result.combine(violations) { it } }
            .zip(alternativeViolations) { result, violations -> result.combine(violations) { it } }
            .map { result -> result.combine(coverageErrors(result)) { it } }
    }

    // The legal address keeps an existing variant its legal entity is still named in, so the variants it carries today
    // cover the header as well as the stated ones.
    private fun coverageErrors(result: ParseResult<LegalEntityUpdateParsed, LegalEntityUpdateError>): List<LegalEntityUpdateError> {
        val parsed = (result as? ParseResult.Success)?.parsed ?: return emptyList()
        val coveredScriptCodes = parsed.content.legalAddress.scriptCodes() + parsed.target.legalAddress.scriptCodes()

        return scriptVariantCoverageValidator.check(coveredScriptCodes, parsed.content.header.scriptCodes())
    }
}
