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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderMetadata
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityHeaderParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityScriptVariant
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.ConfidenceCriteriaParser
import org.eclipse.tractusx.bpdm.pool.util.ValidationLimits
import org.springframework.stereotype.Service

/**
 * Validates the fields of a legal-entity header against the metadata they reference — legal forms, identifier types and
 * script codes. The header only: the legal address is validated separately, and of the identifiers only their number is
 * checked here, because uniqueness needs an owner BPN this parser does not see.
 */
@Service
class LegalEntityHeaderParser(
    private val legalFormRepository: LegalFormRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val scriptCodeRepository: ScriptCodeRepository,
    private val confidenceCriteriaParser: ConfidenceCriteriaParser
) {

    /**
     * Validates each header and reports either the validated header or every problem found in that entry.
     */
    fun parse(headers: List<LegalEntityHeaderRequest>): List<ParseResult<LegalEntityHeaderParsed, LegalEntityHeaderParseError>> {
        val metadata = fetchMetadata(headers)
        return headers.map { parseEntry(it, metadata) }
    }

    private fun fetchMetadata(headers: List<LegalEntityHeaderRequest>): LegalEntityHeaderMetadata {
        val legalFormKeys = headers.mapNotNull { it.legalForm }.toSet()
        val idTypeKeys = headers.flatMap { it.identifiers }.mapNotNull { it.type }.toSet()
        val scriptCodeKeys = headers.flatMap { it.scriptVariants }.map { it.scriptCode }.toSet()

        return LegalEntityHeaderMetadata(
            legalForms = legalFormRepository.findByTechnicalKeyIn(legalFormKeys).associateBy { it.technicalKey },
            idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.LEGAL_ENTITY, idTypeKeys)
                .associateBy { it.technicalKey },
            scriptCodes = scriptCodeRepository.findByTechnicalKeyIn(scriptCodeKeys).associateBy { it.technicalKey }
        )
    }

    private fun parseEntry(header: LegalEntityHeaderRequest, metadata: LegalEntityHeaderMetadata): ParseResult<LegalEntityHeaderParsed, LegalEntityHeaderParseError> {
        val errors = mutableListOf<LegalEntityHeaderParseError>()

        val legalName = header.legalName ?: run { errors.add(LegalEntityHeaderParseError.NameMissing); null }
        val legalForm = parseLegalForm(header.legalForm, metadata, errors)
        val confidence = when (val result = confidenceCriteriaParser.parse(header.confidenceCriteria, LegalEntityHeaderParseError.ConfidenceCriteriaMissing)) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> { errors += result.errors; null }
        }
        val identifiers = parseIdentifiers(header.identifiers, metadata, errors)
        val scriptVariants = parseScriptVariants(header.scriptVariants, metadata, errors)

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        // No errors guarantees the nullable sub-results above are present.
        return ParseResult.Success(
            LegalEntityHeaderParsed(
                legalName = legalName!!,
                legalShortName = header.legalShortName,
                legalForm = legalForm,
                identifiers = identifiers,
                states = header.states,
                confidenceCriteria = confidence!!,
                isDataSpaceParticipant = header.isDataSpaceParticipant,
                ownershipUltimate = header.ownershipUltimate,
                scriptVariants = scriptVariants
            )
        )
    }

    private fun parseLegalForm(
        legalForm: String?,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityHeaderParseError>
    ): LegalFormDb? {
        if (legalForm == null) return null
        return metadata.legalForms[legalForm] ?: run { errors.add(LegalEntityHeaderParseError.LegalFormNotFound(legalForm)); null }
    }

    private fun parseIdentifiers(
        requests: List<LegalEntityIdentifier>,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityHeaderParseError>
    ): List<LegalEntityIdentifierParsed> {
        if (requests.size > ValidationLimits.IDENTIFIER_AMOUNT_LIMIT) {
            errors.add(LegalEntityHeaderParseError.IdentifiersTooMany(requests.size))
        }
        return requests.mapIndexedNotNull { index, request ->
            val value = request.value ?: run { errors.add(LegalEntityHeaderParseError.IdentifierValueMissing(index)); null }
            val type = request.type
            val typeEntity = when {
                type == null -> { errors.add(LegalEntityHeaderParseError.IdentifierTypeMissing(index)); null }
                else -> metadata.idTypes[type] ?: run { errors.add(LegalEntityHeaderParseError.IdentifierTypeNotFound(index, type)); null }
            }
            if (value == null || typeEntity == null) null else LegalEntityIdentifierParsed(value, typeEntity, request.issuingBody)
        }
    }

    private fun parseScriptVariants(
        requests: List<LegalEntityScriptVariant>,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityHeaderParseError>
    ): List<LegalEntityScriptVariantParsed> {
        val claimedScriptCodes = mutableSetOf<String>()
        return requests.mapIndexedNotNull { index, variant ->
            if (!claimedScriptCodes.add(variant.scriptCode)) {
                errors.add(LegalEntityHeaderParseError.ScriptVariantDuplicateScriptCode(index, variant.scriptCode))
                null
            } else {
                parseScriptVariant(index, variant, metadata, errors)
            }
        }
    }

    private fun parseScriptVariant(
        index: Int,
        variant: LegalEntityScriptVariant,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityHeaderParseError>
    ): LegalEntityScriptVariantParsed? {
        val scriptCode = metadata.scriptCodes[variant.scriptCode]
            ?: run { errors.add(LegalEntityHeaderParseError.ScriptCodeNotFound(index, variant.scriptCode)); null }
        val legalName = variant.legalName?.takeIf { it.isNotBlank() }
            ?: run { errors.add(LegalEntityHeaderParseError.ScriptVariantLegalNameMissing(index)); null }

        if (scriptCode == null || legalName == null) return null

        return LegalEntityScriptVariantParsed(scriptCode, legalName, variant.shortName)
    }
}