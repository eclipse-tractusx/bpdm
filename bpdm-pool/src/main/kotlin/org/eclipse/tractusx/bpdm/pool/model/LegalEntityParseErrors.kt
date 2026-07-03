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

package org.eclipse.tractusx.bpdm.pool.model

sealed interface LegalEntityCreateParseError

sealed interface LegalEntityUpdateParseError

/**
 * Errors produced by parsing legal-entity *header* content (everything but the legal address). As a subtype of both legal-
 * entity operations from a single definition, the same content errors flow into create and update without wrapping; the
 * legal address contributes its own [AddressContentParseError], which is likewise a legal-entity error directly.
 *
 * Kept flat (no Field/Metadata/Constraint sub-grouping like the address errors) because legal-entity create and update
 * share the whole set and no caller needs to match a sub-group. The identifier presence errors and `DuplicateIdentifier`
 * carry the offending entry's `index`; `DuplicateIdentifier` is produced by [org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityIdentifierDuplicateValidator]
 * (it needs the owner BPN), the rest by the header parser.
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
