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

sealed interface AddressCreateParseError

sealed interface AddressUpdateParseError

/**
 * Errors produced by parsing address *content* (the shared content parser). Each case is, from a single definition, a
 * subtype of every operation that embeds an address: standalone address create/update, a site's main address and a legal
 * entity's legal address. So a site's main-address or a legal entity's legal-address content errors are that operation's
 * errors directly — no wrapping, callers match them flatly or via a single `is AddressContentParseError` branch.
 */
sealed interface AddressContentParseError :
    AddressCreateParseError,
    AddressUpdateParseError,
    SiteCreateParseError,
    SiteUpdateParseError,
    LegalEntityCreateParseError,
    LegalEntityUpdateParseError

/**
 * Field-presence/format errors. Cases mirror the address-relevant `TaskStepBuildService.CleaningError` entries.
 */
sealed interface AddressFieldParseError : AddressContentParseError {
    data object PhysicalCountryMissing : AddressFieldParseError
    data object PhysicalCityMissing : AddressFieldParseError
    data object AlternativeCountryMissing : AddressFieldParseError
    data object AlternativeCityMissing : AddressFieldParseError
    data object AlternativeDeliveryServiceTypeMissing : AddressFieldParseError
    data object AlternativeDeliveryServiceNumberMissing : AddressFieldParseError
    data object ConfidenceCriteriaMissing : AddressFieldParseError
    data class CountryCodeNotRecognized(val value: String) : AddressFieldParseError
    data class IdentifierValueMissing(val index: Int) : AddressFieldParseError
    data class IdentifierTypeMissing(val index: Int) : AddressFieldParseError
    data class StateTypeMissing(val index: Int) : AddressFieldParseError
}

/**
 * Metadata-resolution errors: a referenced metadata key exists in the request but no matching entity is registered.
 * Distinct from [AddressFieldParseError] (presence/format) since these are lookups against persisted metadata.
 */
sealed interface AddressMetadataParseError : AddressContentParseError {
    data class IdentifierTypeNotFound(val index: Int, val type: String) : AddressMetadataParseError
    data class PhysicalRegionNotFound(val regionCode: String) : AddressMetadataParseError
    data class AlternativeRegionNotFound(val regionCode: String) : AddressMetadataParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : AddressMetadataParseError
}

/**
 * Cardinality/uniqueness constraint violations. `IdentifiersTooMany` is content-intrinsic (produced by the content parser);
 * `DuplicateIdentifier` is identity-aware and DB-backed (produced by the duplicate validator).
 */
sealed interface AddressConstraintParseError : AddressContentParseError {
    data class IdentifiersTooMany(val count: Int) : AddressConstraintParseError
    data class DuplicateIdentifier(val index: Int, val type: String, val value: String) : AddressConstraintParseError
}
