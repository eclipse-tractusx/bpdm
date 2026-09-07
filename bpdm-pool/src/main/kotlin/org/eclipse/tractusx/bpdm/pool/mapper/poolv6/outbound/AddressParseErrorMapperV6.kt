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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the address services' sealed parse errors to the v6 `/addresses` [ErrorInfoV6] codes.
 *
 * The v6 error enums are frozen and predate script variants, so every script-variant error is thrown as an internal
 * error instead of getting a public code. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class AddressParseErrorMapperV6 {

    fun toCreateErrorInfo(error: AddressCreateParseError, entityKey: String?): ErrorInfoV6<AddressCreateErrorV6> =
        when (error) {
            is InvalidParentBpn ->
                ErrorInfoV6(AddressCreateErrorV6.BpnNotValid, "Parent '${error.bpn}' is not a valid BPNL/BPNS", entityKey)
            is UnresolvableLegalEntity ->
                ErrorInfoV6(AddressCreateErrorV6.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is UnresolvableSite ->
                ErrorInfoV6(AddressCreateErrorV6.SiteNotFound, "Parent site '${error.bpn}' not found", entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV6(
                    AddressCreateErrorV6.SiteNotInLegalEntity,
                    "Parent site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressCreateErrorV6.RegionNotFound,
                identifierNotFound = AddressCreateErrorV6.IdentifierNotFound,
                duplicateIdentifier = AddressCreateErrorV6.AddressDuplicateIdentifier,
                identifiersTooMany = AddressCreateErrorV6.IdentifiersTooMany
            )
        }

    fun toUpdateErrorInfo(error: AddressUpdateParseError, entityKey: String?): ErrorInfoV6<AddressUpdateErrorV6> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfoV6(AddressUpdateErrorV6.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressUpdateErrorV6.RegionNotFound,
                identifierNotFound = AddressUpdateErrorV6.IdentifierNotFound,
                duplicateIdentifier = AddressUpdateErrorV6.AddressDuplicateIdentifier,
                identifiersTooMany = AddressUpdateErrorV6.IdentifiersTooMany
            )
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV6(
                    AddressUpdateErrorV6.SiteNotInLegalEntity,
                    "Site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            // Reachable over v6: a v6 write sends no script variants, so it can drop coverage another business partner
            // still needs. The frozen v6 enum has no code for it, so the client gets an internal error.
            is ScriptVariantCoverageStillNeeded -> throw internalError(error)
            is UnresolvableSite,
            is SiteMainAddressOmitted,
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
        }

    fun toLegalEntityCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV6<LegalEntityCreateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateErrorV6.LegalAddressRegionNotFound,
            identifierNotFound = LegalEntityCreateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateErrorV6.LegalAddressIdentifiersTooMany
        )

    fun toLegalEntityUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV6<LegalEntityUpdateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateErrorV6.LegalAddressRegionNotFound,
            identifierNotFound = LegalEntityUpdateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateErrorV6.LegalAddressIdentifiersTooMany
        )

    fun toSiteCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV6<SiteCreateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateErrorV6.MainAddressRegionNotFound,
            identifierNotFound = SiteCreateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateErrorV6.MainAddressIdentifiersTooMany
        )

    fun toSiteUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV6<SiteUpdateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateErrorV6.MainAddressRegionNotFound,
            identifierNotFound = SiteUpdateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateErrorV6.MainAddressIdentifiersTooMany
        )

    private fun <E : ErrorCodeV6> sharedErrorInfo(
        error: AddressContentParseError,
        entityKey: String?,
        regionNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfoV6<E> =
        when (error) {
            is AddressFieldParseError -> throw internalError(error)
            is AddressMetadataParseError -> when (error) {
                is AddressMetadataParseError.IdentifierTypeNotFound ->
                    ErrorInfoV6(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataParseError.PhysicalRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.AlternativeRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintParseError -> when (error) {
                is AddressConstraintParseError.IdentifiersTooMany ->
                    ErrorInfoV6(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintParseError.DuplicateIdentifier ->
                    ErrorInfoV6(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantParseError -> throw internalError(error)
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected address validation error that has no v6 client error code: $error")
}
