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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityUpdateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableLegalEntity
import org.springframework.stereotype.Component

/**
 * Translates the legal-entity services' sealed parse errors into the version-specific [ErrorInfo] codes the
 * `/legal-entities` endpoints return. Target resolution and the embedded legal-address errors (delegated to
 * [AddressParseErrorMapper], mapped to the `LegalAddress*` codes) plus the legal-entity header content errors cover all
 * reachable cases.
 *
 * Of the [LegalEntityContentParseError] cases only legal form, identifier type, duplicate identifier and too-many
 * identifiers have a public `LegalEntity*Error` code. The presence errors (name/confidence/identifier value+type) are
 * guaranteed absent by the bounded REST DTO, and an unknown header script code previously NPE'd to a 500 — either way
 * those are treated as internal errors. The `when` blocks are exhaustive so a newly added parse error fails to compile
 * here until it is given a code.
 */
@Component
class LegalEntityParseErrorMapper(
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    fun toCreateErrorInfo(error: LegalEntityCreateParseError, entityKey: String?): ErrorInfo<LegalEntityCreateError> =
        when (error) {
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityCreateErrorInfo(error, entityKey)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateError.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateError.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateError.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateError.LegalEntityIdentifiersTooMany
            )
        }

    fun toUpdateErrorInfo(error: LegalEntityUpdateParseError, entityKey: String?): ErrorInfo<LegalEntityUpdateError> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfo(LegalEntityUpdateError.LegalEntityNotFound, "Legal entity '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toLegalEntityUpdateErrorInfo(error, entityKey)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateError.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateError.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateError.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateError.LegalEntityIdentifiersTooMany
            )
        }

    private fun <E : ErrorCode> contentErrorInfo(
        error: LegalEntityContentParseError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfo<E> =
        when (error) {
            is LegalEntityContentParseError.LegalFormNotFound ->
                ErrorInfo(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityContentParseError.IdentifierTypeNotFound ->
                ErrorInfo(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityContentParseError.DuplicateIdentifier ->
                ErrorInfo(duplicateIdentifier, "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            is LegalEntityContentParseError.IdentifiersTooMany ->
                ErrorInfo(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityContentParseError.NameMissing,
            is LegalEntityContentParseError.ConfidenceCriteriaMissing,
            is LegalEntityContentParseError.IdentifierValueMissing,
            is LegalEntityContentParseError.IdentifierTypeMissing,
            is LegalEntityContentParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: LegalEntityContentParseError) =
        BpdmValidationException("Unexpected legal entity content parse error (no public error code): $error")
}
