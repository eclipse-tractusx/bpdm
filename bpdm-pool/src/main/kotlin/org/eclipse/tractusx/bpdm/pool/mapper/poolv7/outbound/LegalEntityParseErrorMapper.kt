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

import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the legal-entity services' sealed parse errors to the `/legal-entities` [ErrorInfo] codes, delegating
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

    fun toCreateErrorInfo(error: LegalEntityCreateParseError, entityKey: String?): ErrorInfo<LegalEntityCreateError> =
        when (error) {
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityCreateErrorInfo(error, entityKey)
            is LegalEntityHeaderParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateError.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateError.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateError.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateError.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityCreateError.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityCreateError.ScriptVariantDuplicateScriptCode
            )
        }

    fun toUpdateErrorInfo(error: LegalEntityUpdateParseError, entityKey: String?): ErrorInfo<LegalEntityUpdateError> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "Legal entity '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is MultipleUltimateOwnersInHierarchy -> ErrorInfo(
                LegalEntityUpdateError.MultipleUltimateOwnersInHierarchy,
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}",
                entityKey
            )
            is AlternativeHeadquarterCannotOwnUltimately -> ErrorInfo(
                LegalEntityUpdateError.AlternativeHeadquarterCannotOwnUltimately,
                "Legal entity '${error.bpnl}' cannot carry the ultimate-owner flag because it is an alternative headquarter",
                entityKey
            )
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityUpdateErrorInfo(error, entityKey)
            is LegalEntityHeaderParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateError.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateError.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateError.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateError.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityUpdateError.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityUpdateError.ScriptVariantDuplicateScriptCode
            )
        }

    private fun <E : ErrorCode> contentErrorInfo(
        error: LegalEntityHeaderParseError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantLegalNameMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfo<E> =
        when (error) {
            is LegalEntityHeaderParseError.LegalFormNotFound ->
                ErrorInfo(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityHeaderParseError.IdentifierTypeNotFound ->
                ErrorInfo(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityHeaderParseError.DuplicateIdentifier ->
                ErrorInfo(duplicateIdentifier, "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            is LegalEntityHeaderParseError.IdentifiersTooMany ->
                ErrorInfo(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityHeaderParseError.ScriptVariantLegalNameMissing ->
                ErrorInfo(scriptVariantLegalNameMissing, "Script variant ${error.index} has no legal name", entityKey)
            is LegalEntityHeaderParseError.ScriptVariantDuplicateScriptCode ->
                ErrorInfo(scriptVariantDuplicateScriptCode, "Duplicate legal entity script variant for script code '${error.scriptCode}'", entityKey)
            is LegalEntityHeaderParseError.NameMissing,
            is LegalEntityHeaderParseError.ConfidenceCriteriaMissing,
            is LegalEntityHeaderParseError.IdentifierValueMissing,
            is LegalEntityHeaderParseError.IdentifierTypeMissing,
            is LegalEntityHeaderParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: LegalEntityCreateParseError) =
        BpdmValidationException("Unexpected legal entity parse error (no public error code): $error")
}
