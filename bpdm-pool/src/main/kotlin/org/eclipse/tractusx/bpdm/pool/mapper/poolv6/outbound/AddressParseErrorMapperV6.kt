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

import org.eclipse.tractusx.bpdm.pool.api.v6.exception.AddressCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.AddressUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.ErrorCodeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.*
import org.springframework.stereotype.Component

/**
 * Maps the address services' sealed parse errors to the v6 `/addresses` [ErrorInfoV6] codes.
 *
 * The v6 error enums are frozen and predate script variants, so every script-variant error is thrown as an internal
 * error instead of getting a public code. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class AddressParseErrorMapperV6 {

    fun toCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<AddressCreateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressCreateErrorV6.RegionNotFound,
            countryNotFound = AddressCreateErrorV6.CountryNotFound,
            identifierNotFound = AddressCreateErrorV6.IdentifierNotFound,
            duplicateIdentifier = AddressCreateErrorV6.AddressDuplicateIdentifier,
            identifiersTooMany = AddressCreateErrorV6.IdentifiersTooMany
        )

    fun toUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<AddressUpdateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressUpdateErrorV6.RegionNotFound,
            countryNotFound = AddressUpdateErrorV6.CountryNotFound,
            identifierNotFound = AddressUpdateErrorV6.IdentifierNotFound,
            duplicateIdentifier = AddressUpdateErrorV6.AddressDuplicateIdentifier,
            identifiersTooMany = AddressUpdateErrorV6.IdentifiersTooMany
        )

    fun toLegalEntityCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<LegalEntityCreateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateErrorV6.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityCreateErrorV6.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityCreateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateErrorV6.LegalAddressIdentifiersTooMany
        )

    fun toLegalEntityUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<LegalEntityUpdateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateErrorV6.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityUpdateErrorV6.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityUpdateErrorV6.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateErrorV6.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateErrorV6.LegalAddressIdentifiersTooMany
        )

    fun toSiteCreateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<SiteCreateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateErrorV6.MainAddressRegionNotFound,
            countryNotFound = SiteCreateErrorV6.MainAddressCountryNotFound,
            identifierNotFound = SiteCreateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateErrorV6.MainAddressIdentifiersTooMany
        )

    fun toSiteUpdateErrorInfo(error: AddressContentError, entityKey: String?): ErrorInfoV6<SiteUpdateErrorV6> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateErrorV6.MainAddressRegionNotFound,
            countryNotFound = SiteUpdateErrorV6.MainAddressCountryNotFound,
            identifierNotFound = SiteUpdateErrorV6.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateErrorV6.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateErrorV6.MainAddressIdentifiersTooMany
        )

    private fun <E : ErrorCodeV6> sharedErrorInfo(
        error: AddressContentError,
        entityKey: String?,
        regionNotFound: E,
        countryNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfoV6<E> =
        when (error) {
            is AddressFieldError -> throw internalError(error)
            is AddressMetadataError -> when (error) {
                is AddressMetadataError.IdentifierTypeNotFound ->
                    ErrorInfoV6(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataError.PhysicalCountryNotFound ->
                    ErrorInfoV6(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.AlternativeCountryNotFound ->
                    ErrorInfoV6(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataError.PhysicalRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.AlternativeRegionNotFound ->
                    ErrorInfoV6(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintError -> when (error) {
                is AddressConstraintError.IdentifiersTooMany ->
                    ErrorInfoV6(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintError.DuplicateIdentifier ->
                    ErrorInfoV6(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantError -> throw internalError(error)
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected address validation error that has no v6 client error code: $error")
}
