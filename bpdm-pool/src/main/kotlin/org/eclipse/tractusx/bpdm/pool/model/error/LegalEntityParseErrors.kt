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

package org.eclipse.tractusx.bpdm.pool.model.error

sealed interface LegalEntityCreateParseError


sealed interface LegalEntityUpdateParseError


sealed interface LegalEntityGetParseError

data class UnresolvableLegalEntityIdentifier(val identifierTypeKey: String, val identifierValue: String) : LegalEntityGetParseError

/**
 * The problems the content of one legal entity update can be faulted for: what any write of that content can be
 * faulted for, plus the rules that only hold once the legal entity exists.
 */
sealed interface LegalEntityUpdateContentParseError : LegalEntityUpdateParseError

/**
 * The problems writing the ultimate-owner flag onto a legal entity that already exists can be faulted for.
 */
sealed interface LegalEntityOwnershipParseError : LegalEntityUpdateContentParseError

/**
 * More than one legal entity in the same ownership tree would carry the ultimate-owner flag, whether a flag is set or an
 * ownership joins two trees. Among legal entity writes it is update-only: a legal entity being created has no ownership
 * relations yet, so its tree is itself.
 */
data class MultipleUltimateOwnersInHierarchy(val conflictingBpnls: List<String>) : LegalEntityOwnershipParseError,
    OwnershipUpsertParseError

/**
 * An alternative headquarter cannot carry the ultimate-owner flag. Update-only: setting the flag on an alternative is rejected,
 * but clearing it stays allowed.
 */
data class AlternativeHeadquarterCannotOwnUltimately(val bpnl: String) : LegalEntityOwnershipParseError

/**
 * The problems the content of one legal entity can be faulted for: its header and its legal address.
 */
sealed interface LegalEntityContentParseError : LegalEntityCreateParseError, LegalEntityUpdateContentParseError

/**
 * Legal-entity header parse errors, shared by create and update. Kept flat (unlike the address errors' Field/Metadata/
 * Constraint grouping) since no caller matches a sub-group. The legal address contributes its own content errors
 * directly.
 */
sealed interface LegalEntityHeaderParseError : LegalEntityContentParseError {
    data object NameMissing : LegalEntityHeaderParseError
    data object ConfidenceCriteriaMissing : LegalEntityHeaderParseError
    data class LegalFormNotFound(val legalForm: String) : LegalEntityHeaderParseError
    data class IdentifierValueMissing(val index: Int) : LegalEntityHeaderParseError
    data class IdentifierTypeMissing(val index: Int) : LegalEntityHeaderParseError
    data class IdentifierTypeNotFound(val index: Int, val type: String) : LegalEntityHeaderParseError
    data class IdentifiersTooMany(val count: Int) : LegalEntityHeaderParseError
    data class DuplicateIdentifier(val index: Int, val type: String, val value: String) : LegalEntityHeaderParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : LegalEntityHeaderParseError
    data class ScriptVariantLegalNameMissing(val index: Int) : LegalEntityHeaderParseError
    data class ScriptVariantDuplicateScriptCode(val index: Int, val scriptCode: String) : LegalEntityHeaderParseError
}
