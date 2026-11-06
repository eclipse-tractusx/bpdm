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

package org.eclipse.tractusx.bpdm.pool.model.parsed.error

sealed interface LegalEntityCreateError

sealed interface LegalEntityUpdateError

/**
 * More than one legal entity in the same ownership tree would carry the ultimate-owner flag. Update-only: a legal entity
 * being created has no ownership relations yet, so its tree is itself.
 */
data class MultipleUltimateOwnersInHierarchy(val conflictingBpnls: List<String>) : LegalEntityUpdateError

/**
 * An alternative headquarter cannot carry the ultimate-owner flag. Update-only: setting the flag on an alternative is rejected,
 * but clearing it stays allowed.
 */
data class AlternativeHeadquarterCannotOwnUltimately(val bpnl: String) : LegalEntityUpdateError

/**
 * Legal-entity header parse errors, shared by create and update. Kept flat (unlike the address errors' Field/Metadata/
 * Constraint grouping) since no caller matches a sub-group. The legal address contributes its own
 * [AddressContentError] directly.
 */
sealed interface LegalEntityContentError : LegalEntityCreateError, LegalEntityUpdateError {
    data object NameMissing : LegalEntityContentError
    data object ConfidenceCriteriaMissing : LegalEntityContentError
    data class LegalFormNotFound(val legalForm: String) : LegalEntityContentError
    data class IdentifierValueMissing(val index: Int) : LegalEntityContentError
    data class IdentifierTypeMissing(val index: Int) : LegalEntityContentError
    data class IdentifierTypeNotFound(val index: Int, val type: String) : LegalEntityContentError
    data class IdentifiersTooMany(val count: Int) : LegalEntityContentError
    data class DuplicateIdentifier(val index: Int, val type: String, val value: String) : LegalEntityContentError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : LegalEntityContentError
    data class ScriptVariantLegalNameMissing(val index: Int) : LegalEntityContentError
    data class ScriptVariantDuplicateScriptCode(val index: Int, val scriptCode: String) : LegalEntityContentError
}
