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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the legal-entity services' sealed parse errors to the `/legal-entities` [ErrorInfoV7] codes, delegating
 * legal-address errors to [AddressParseErrorMapper].
 *
 * An error the bounded DTO already rules out, or that this operation cannot reach, gets no public code and is thrown as
 * an internal error instead; script-variant content is client-nullable and therefore does get public codes. The `when`s
 * are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class LegalEntityParseErrorMapper(
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    fun toCreateErrorInfo(error: LegalEntityCreateParseError, entityKey: String?): ErrorInfoV7<LegalEntityCreateErrorV7> =
        when (error) {
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityCreateErrorInfo(error, entityKey)
            is ScriptVariantCoverageParseError -> throw internalError(error)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateErrorV7.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateErrorV7.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateErrorV7.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateErrorV7.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityCreateErrorV7.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityCreateErrorV7.ScriptVariantDuplicateScriptCode
            )
        }

    fun toUpdateErrorInfo(error: LegalEntityUpdateParseError, entityKey: String?): ErrorInfoV7<LegalEntityUpdateErrorV7> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV7(LegalEntityUpdateErrorV7.LegalEntityNotFound, "Legal entity '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is MultipleUltimateOwnersInHierarchy -> ErrorInfoV7(
                LegalEntityUpdateErrorV7.MultipleUltimateOwnersInHierarchy,
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}",
                entityKey
            )
            is AlternativeHeadquarterCannotOwnUltimately -> ErrorInfoV7(
                LegalEntityUpdateErrorV7.AlternativeHeadquarterCannotOwnUltimately,
                "Legal entity '${error.bpnl}' cannot carry the ultimate-owner flag because it is an alternative headquarter",
                entityKey
            )
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityUpdateErrorInfo(error, entityKey)
            is ScriptVariantCoverageStillNeeded ->
                ErrorInfoV7(
                    LegalEntityUpdateErrorV7.ScriptVariantCoverageStillNeeded,
                    "Script code '${error.scriptCode}' must stay covered by the legal address: business partner " +
                            "'${error.requiredByBpn}' is named in that script",
                    entityKey
                )
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateErrorV7.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateErrorV7.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateErrorV7.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateErrorV7.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityUpdateErrorV7.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityUpdateErrorV7.ScriptVariantDuplicateScriptCode
            )
        }

    private fun <E : ErrorCodeV7> contentErrorInfo(
        error: LegalEntityContentParseError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantLegalNameMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfoV7<E> =
        when (error) {
            is LegalEntityContentParseError.LegalFormNotFound ->
                ErrorInfoV7(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityContentParseError.IdentifierTypeNotFound ->
                ErrorInfoV7(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityContentParseError.DuplicateIdentifier ->
                ErrorInfoV7(duplicateIdentifier, "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            is LegalEntityContentParseError.IdentifiersTooMany ->
                ErrorInfoV7(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing ->
                ErrorInfoV7(scriptVariantLegalNameMissing, "Script variant ${error.index} has no legal name", entityKey)
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode ->
                ErrorInfoV7(scriptVariantDuplicateScriptCode, "Duplicate legal entity script variant for script code '${error.scriptCode}'", entityKey)
            is LegalEntityContentParseError.NameMissing,
            is LegalEntityContentParseError.ConfidenceCriteriaMissing,
            is LegalEntityContentParseError.IdentifierValueMissing,
            is LegalEntityContentParseError.IdentifierTypeMissing,
            is LegalEntityContentParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: LegalEntityCreateParseError) =
        BpdmValidationException("Unexpected legal entity parse error (no public error code): $error")
}
