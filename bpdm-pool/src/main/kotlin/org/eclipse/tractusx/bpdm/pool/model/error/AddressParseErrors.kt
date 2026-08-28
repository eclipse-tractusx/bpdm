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

sealed interface AddressSiteMembershipParseError

/**
 * A parent site was referenced that does not belong to the address's own legal entity. A site may only be assigned to an
 * address under the same legal entity. Unlike a resolution failure, both BPNs exist — they just don't belong together.
 */
data class SiteNotInAddressLegalEntity(val siteBpn: String, val legalEntityBpn: String) :
    AddressCreateParseError, AddressUpdateParseError, AddressSiteMembershipParseError

/**
 * A stated site membership left out a site the address is the main address of. Such a site is bound to the address by
 * its own main-address relation, not by the statement, so unlinking it would leave it pointing at an address that does
 * not list it.
 */
data class SiteMainAddressOmitted(val siteBpn: String) :
    AddressUpdateParseError, AddressSiteMembershipParseError

/**
 * Address-content parse errors. Subtypes every operation embedding an address (standalone, site main address, legal
 * address) so they surface as that operation's errors directly — no wrapping, matched flatly.
 */
sealed interface AddressContentParseError :
    AddressCreateParseError,
    AddressUpdateParseError,
    SiteCreateParseError,
    SiteUpdateParseError,
    LegalEntityCreateParseError,
    LegalEntityUpdateParseError

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

sealed interface AddressMetadataParseError : AddressContentParseError {
    data class IdentifierTypeNotFound(val index: Int, val type: String) : AddressMetadataParseError
    data class PhysicalRegionNotFound(val regionCode: String) : AddressMetadataParseError
    data class AlternativeRegionNotFound(val regionCode: String) : AddressMetadataParseError
    data class ScriptCodeNotFound(val index: Int, val scriptCode: String) : AddressMetadataParseError
}

sealed interface AddressConstraintParseError : AddressContentParseError {
    data class IdentifiersTooMany(val count: Int) : AddressConstraintParseError
    data class DuplicateIdentifier(val index: Int, val type: String, val value: String) : AddressConstraintParseError
}

/**
 * Errors of an address's script variants. Kept apart from [AddressFieldParseError] because that group is unreachable by
 * contract and therefore maps to an internal error, while every script-variant field is client-nullable.
 */
sealed interface AddressScriptVariantParseError : AddressContentParseError {
    data class PhysicalCityMissing(val index: Int) : AddressScriptVariantParseError
    data class AlternativeCityMissing(val index: Int) : AddressScriptVariantParseError
    data class DuplicateScriptCode(val index: Int, val scriptCode: String) : AddressScriptVariantParseError
}
