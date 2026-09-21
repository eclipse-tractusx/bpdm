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
import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityCreateEntryParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.springframework.stereotype.Service

/**
 * Validates legal-entity create requests: the header content with its identifier uniqueness and the legal address.
 */
@Service
class LegalEntityCreateParser(
    private val legalEntityContentParser: LegalEntityContentParser,
    private val coverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Validates each request and reports either the validated legal entity or every problem found in that entry.
     */
    fun parse(requests: List<LegalEntityCreateRequest>): List<ParseResult<LegalEntityCreateParsed, LegalEntityCreateParseError>> =
        coverageValidator.applyTo(parseWithoutScriptVariantCoverage(requests), ::coverageWrites) { it }

    /**
     * Validates each request as [parse] does, except for script variant coverage, for a caller that writes further
     * addresses and judges coverage over all of them together.
     */
    fun parseWithoutScriptVariantCoverage(
        requests: List<LegalEntityCreateRequest>
    ): List<ParseResult<LegalEntityCreateParsed, LegalEntityCreateEntryParseError>> {
        val contents = requests.map { it.content }
        val contentResults = legalEntityContentParser.parse(contents, contents.map { null })

        return contentResults.map { result ->
            when (result) {
                is ParseResult.Success -> ParseResult.Success(LegalEntityCreateParsed(result.parsed))
                is ParseResult.Failure -> result
            }
        }
    }

    // What this write leaves behind, as script variant coverage sees it.
    private fun coverageWrites(parsed: LegalEntityCreateParsed): List<AddressCoverageWrite> =
        listOf(
            AddressCoverageWrite.Created(
                partners = listOf(PartnerScriptCodes(bpn = null, parsed.content.header.scriptCodes())),
                scriptCodes = parsed.content.legalAddress.scriptCodes()
            )
        )
}
