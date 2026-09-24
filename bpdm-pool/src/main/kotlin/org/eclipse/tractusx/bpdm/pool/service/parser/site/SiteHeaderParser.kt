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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.pool.entity.ScriptCodeDb
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.SiteHeaderParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteScriptVariant
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.ConfidenceCriteriaParser
import org.springframework.stereotype.Service

/**
 * Validates the fields of a site header against the script codes they reference. The header only: the main address is
 * validated separately.
 */
@Service
class SiteHeaderParser(
    private val scriptCodeRepository: ScriptCodeRepository,
    private val confidenceCriteriaParser: ConfidenceCriteriaParser
) {

    /**
     * Validates each header and reports either the validated header or every problem found in that entry.
     */
    fun parse(headers: List<SiteHeaderRequest>): List<ParseResult<SiteHeaderParsed, SiteHeaderParseError>> {
        val scriptCodes = fetchScriptCodes(headers)
        return headers.map { parseEntry(it, scriptCodes) }
    }

    private fun fetchScriptCodes(headers: List<SiteHeaderRequest>): Map<String, ScriptCodeDb> {
        val keys = headers.flatMap { it.scriptVariants }.map { it.scriptCode }.toSet()
        return scriptCodeRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
    }

    private fun parseEntry(header: SiteHeaderRequest, scriptCodes: Map<String, ScriptCodeDb>): ParseResult<SiteHeaderParsed, SiteHeaderParseError> {
        val errors = mutableListOf<SiteHeaderParseError>()

        val name = header.name ?: run { errors.add(SiteHeaderParseError.NameMissing); null }
        val confidence = when (val result = confidenceCriteriaParser.parse(header.confidenceCriteria, SiteHeaderParseError.ConfidenceCriteriaMissing)) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> { errors += result.errors; null }
        }
        val scriptVariants = parseScriptVariants(header.scriptVariants, scriptCodes, errors)

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        // No errors guarantees the nullable sub-results above are present.
        return ParseResult.Success(
            SiteHeaderParsed(
                name = name!!,
                states = header.states,
                confidenceCriteria = confidence!!,
                scriptVariants = scriptVariants
            )
        )
    }

    private fun parseScriptVariants(
        requests: List<SiteScriptVariant>,
        scriptCodes: Map<String, ScriptCodeDb>,
        errors: MutableList<SiteHeaderParseError>
    ): List<SiteScriptVariantParsed> {
        val claimedScriptCodes = mutableSetOf<String>()
        return requests.mapIndexedNotNull { index, variant ->
            if (!claimedScriptCodes.add(variant.scriptCode)) {
                errors.add(SiteHeaderParseError.ScriptVariantDuplicateScriptCode(index, variant.scriptCode))
                null
            } else {
                parseScriptVariant(index, variant, scriptCodes, errors)
            }
        }
    }

    private fun parseScriptVariant(
        index: Int,
        variant: SiteScriptVariant,
        scriptCodes: Map<String, ScriptCodeDb>,
        errors: MutableList<SiteHeaderParseError>
    ): SiteScriptVariantParsed? {
        val scriptCode = scriptCodes[variant.scriptCode]
            ?: run { errors.add(SiteHeaderParseError.ScriptCodeNotFound(index, variant.scriptCode)); null }
        val name = variant.name.takeIf { it.isNotBlank() }
            ?: run { errors.add(SiteHeaderParseError.ScriptVariantNameMissing(index)); null }

        if (scriptCode == null || name == null) return null

        return SiteScriptVariantParsed(scriptCode, name)
    }
}