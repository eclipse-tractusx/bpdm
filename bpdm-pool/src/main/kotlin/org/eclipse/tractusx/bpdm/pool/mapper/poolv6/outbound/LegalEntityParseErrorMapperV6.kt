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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorCodeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the legal-entity services' sealed parse errors to the v6 `/legal-entities` [ErrorInfoV6] codes, delegating
 * legal-address errors to [AddressParseErrorMapperV6].
 *
 * The v6 error enums are frozen and predate both script variants and ultimate ownership, so those errors are thrown as
 * internal errors instead of getting a public code. The `when`s are exhaustive so a new error won't compile until it
 * gets a code.
 */
@Component
class LegalEntityParseErrorMapperV6(
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6
) {

    fun toCreateErrorInfo(error: LegalEntityCreateParseError, entityKey: String?): ErrorInfoV6<LegalEntityCreateErrorV6> =
        when (error) {
            is AddressContentParseError -> addressParseErrorMapperV6.toLegalEntityCreateErrorInfo(error, entityKey)
            is ScriptVariantCoverageParseError -> throw internalError(error)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateErrorV6.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateErrorV6.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateErrorV6.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateErrorV6.LegalEntityIdentifiersTooMany
            )
        }

    fun toUpdateErrorInfo(error: LegalEntityUpdateParseError, entityKey: String?): ErrorInfoV6<LegalEntityUpdateErrorV6> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV6(
                    LegalEntityUpdateErrorV6.LegalEntityNotFound,
                    "Legal entity '${error.bpn}' can't be updated as it doesn't exist",
                    entityKey
                )
            is AddressContentParseError -> addressParseErrorMapperV6.toLegalEntityUpdateErrorInfo(error, entityKey)
            // A v6 write never sets the ownership flag, so the uniqueness rule it guards cannot be broken from v6.
            is MultipleUltimateOwnersInHierarchy -> throw internalError(error)
            is AlternativeHeadquarterCannotOwnUltimately -> throw internalError(error)
            // Reachable over v6: a v6 write sends no script variants, so it can drop coverage another business partner
            // still needs. The frozen v6 enum has no code for it, so the client gets an internal error.
            is ScriptVariantCoverageStillNeeded -> throw internalError(error)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentParseError -> contentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateErrorV6.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateErrorV6.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateErrorV6.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateErrorV6.LegalEntityIdentifiersTooMany
            )
        }

    private fun <E : ErrorCodeV6> contentErrorInfo(
        error: LegalEntityContentParseError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfoV6<E> =
        when (error) {
            is LegalEntityContentParseError.LegalFormNotFound ->
                ErrorInfoV6(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityContentParseError.IdentifierTypeNotFound ->
                ErrorInfoV6(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityContentParseError.DuplicateIdentifier ->
                ErrorInfoV6(
                    duplicateIdentifier,
                    "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'",
                    entityKey
                )
            is LegalEntityContentParseError.IdentifiersTooMany ->
                ErrorInfoV6(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing,
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode,
            is LegalEntityContentParseError.NameMissing,
            is LegalEntityContentParseError.ConfidenceCriteriaMissing,
            is LegalEntityContentParseError.IdentifierValueMissing,
            is LegalEntityContentParseError.IdentifierTypeMissing,
            is LegalEntityContentParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected legal entity parse error (no v6 client error code): $error")
}
