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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityContentParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderMetadata
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityIdentifier
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.eclipse.tractusx.bpdm.pool.util.ValidationLimits
import org.springframework.stereotype.Service

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
/**
 * Validates loose [org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest] content into the bounded, metadata-resolved [org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed] — the
 * legal-entity counterpart of [org.eclipse.tractusx.bpdm.pool.service.LogisticAddressRequestParser], covering only the legal-entity header (the legal address is
 * parsed separately by the address content parser and recombined by the legal-entity service). All errors are accumulated
 * (not fail-fast) so one entry's report is complete. Legal forms, identifier types and script codes are resolved for the
 * whole batch in one query each, then each entry is validated against them. The identifier uniqueness check lives in
 * [LegalEntityIdentifierDuplicateValidator] (it needs the owner BPN); only the size limit is checked here.
 */
@Service
class LegalEntityHeaderParser(
    private val legalFormRepository: LegalFormRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val scriptCodeRepository: ScriptCodeRepository
) {

    fun parse(headers: List<LegalEntityHeaderRequest>): List<ParseResult<LegalEntityHeaderParsed, LegalEntityContentParseError>> {
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

    private fun parseEntry(header: LegalEntityHeaderRequest, metadata: LegalEntityHeaderMetadata): ParseResult<LegalEntityHeaderParsed, LegalEntityContentParseError> {
        val errors = mutableListOf<LegalEntityContentParseError>()

        val legalName = header.legalName ?: run { errors.add(LegalEntityContentParseError.NameMissing); null }
        val legalForm = parseLegalForm(header.legalForm, metadata, errors)
        val confidence = parseConfidence(header.confidenceCriteria, errors)
        val identifiers = parseIdentifiers(header.identifiers, metadata, errors)
        val scriptVariants = header.scriptVariants.mapIndexedNotNull { index, variant -> parseScriptVariant(index, variant, metadata, errors) }

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
                isParticipantData = header.isParticipantData,
                scriptVariants = scriptVariants
            )
        )
    }

    private fun parseLegalForm(
        legalForm: String?,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityContentParseError>
    ): LegalFormDb? {
        if (legalForm == null) return null
        return metadata.legalForms[legalForm] ?: run { errors.add(LegalEntityContentParseError.LegalFormNotFound(legalForm)); null }
    }

    private fun parseConfidence(
        request: ConfidenceCriteriaRequest,
        errors: MutableList<LegalEntityContentParseError>
    ): ConfidenceCriteriaParsed? {
        val sharedByOwner = request.sharedByOwner
        val checkedByExternalDataSource = request.checkedByExternalDataSource
        val lastConfidenceCheckAt = request.lastConfidenceCheckAt
        val nextConfidenceCheckAt = request.nextConfidenceCheckAt

        if (sharedByOwner == null || checkedByExternalDataSource == null ||
            lastConfidenceCheckAt == null || nextConfidenceCheckAt == null
        ) {
            errors.add(LegalEntityContentParseError.ConfidenceCriteriaMissing)
            return null
        }

        return ConfidenceCriteriaParsed(sharedByOwner, checkedByExternalDataSource, lastConfidenceCheckAt, nextConfidenceCheckAt)
    }

    private fun parseIdentifiers(
        requests: List<LegalEntityIdentifier>,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityContentParseError>
    ): List<LegalEntityIdentifierParsed> {
        if (requests.size > ValidationLimits.IDENTIFIER_AMOUNT_LIMIT) {
            errors.add(LegalEntityContentParseError.IdentifiersTooMany(requests.size))
        }
        return requests.mapIndexedNotNull { index, request ->
            val value = request.value ?: run { errors.add(LegalEntityContentParseError.IdentifierValueMissing(index)); null }
            val type = request.type
            val typeEntity = when {
                type == null -> { errors.add(LegalEntityContentParseError.IdentifierTypeMissing(index)); null }
                else -> metadata.idTypes[type] ?: run { errors.add(LegalEntityContentParseError.IdentifierTypeNotFound(index, type)); null }
            }
            if (value == null || typeEntity == null) null else LegalEntityIdentifierParsed(value, typeEntity, request.issuingBody)
        }
    }

    private fun parseScriptVariant(
        index: Int,
        variant: LegalEntityScriptVariant,
        metadata: LegalEntityHeaderMetadata,
        errors: MutableList<LegalEntityContentParseError>
    ): LegalEntityScriptVariantParsed? {
        val scriptCode = metadata.scriptCodes[variant.scriptCode]
            ?: run { errors.add(LegalEntityContentParseError.ScriptCodeNotFound(index, variant.scriptCode)); return null }
        return LegalEntityScriptVariantParsed(scriptCode, variant.legalName, variant.shortName)
    }
}