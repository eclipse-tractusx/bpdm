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

/**
 * More than one legal entity in the same ownership tree would carry the ultimate-owner flag. Update-only: a legal entity
 * being created has no ownership relations yet, so its tree is itself.
 */
data class MultipleUltimateOwnersInHierarchy(val conflictingBpnls: List<String>) : LegalEntityUpdateParseError

/**
 * Legal-entity header parse errors, shared by create and update. Kept flat (unlike the address errors' Field/Metadata/
 * Constraint grouping) since no caller matches a sub-group. The legal address contributes its own
 * [AddressContentParseError] directly.
 */
sealed interface LegalEntityContentParseError : LegalEntityCreateParseError, LegalEntityUpdateParseError {
    data object NameMissing : LegalEntityContentParseError
    data object ConfidenceCriteriaMissing : LegalEntityContentParseError
    data class LegalFormNotFound(val legalForm: String) : LegalEntityContentParseError
    data class IdentifierValueMissing(val index: Int) : LegalEntityContentParseError
    data class IdentifierTypeMissing(val index: Int) : LegalEntityContentParseError
    data class IdentifierTypeNotFound(val index: Int, val type: String) : LegalEntityContentParseError
    data class IdentifiersTooMany(val count: Int) : LegalEntityContentParseError
    data class DuplicateIdentifier(val index: Int, val type: String, val value: String) : LegalEntityContentParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : LegalEntityContentParseError
}
