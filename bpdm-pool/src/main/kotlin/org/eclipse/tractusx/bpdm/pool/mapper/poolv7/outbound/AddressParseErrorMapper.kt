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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.pool.api.v7.exception.AddressCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.AddressUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the address services' sealed parse errors to the `/addresses` [ErrorInfoV7] codes.
 *
 * An error the bounded DTO already rules out, or that this operation cannot reach, gets no public code and is thrown as
 * an internal error instead. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class AddressParseErrorMapper {

    fun toCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV7<AddressCreateErrorV7> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = AddressCreateErrorV7.RegionNotFound,
            countryNotFound = AddressCreateErrorV7.CountryNotFound,
            identifierNotFound = AddressCreateErrorV7.IdentifierNotFound,
            duplicateIdentifier = AddressCreateErrorV7.AddressDuplicateIdentifier,
            identifiersTooMany = AddressCreateErrorV7.IdentifiersTooMany,
            scriptVariantCityMissing = AddressCreateErrorV7.ScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = AddressCreateErrorV7.ScriptVariantDuplicateScriptCode
        )

    fun toUpdateErrorInfo(error: AddressUpdateParseError, entityKey: String?): ErrorInfoV7<AddressUpdateErrorV7> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfoV7(AddressUpdateErrorV7.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressUpdateErrorV7.RegionNotFound,
                countryNotFound = AddressUpdateErrorV7.CountryNotFound,
                identifierNotFound = AddressUpdateErrorV7.IdentifierNotFound,
                duplicateIdentifier = AddressUpdateErrorV7.AddressDuplicateIdentifier,
                identifiersTooMany = AddressUpdateErrorV7.IdentifiersTooMany,
                scriptVariantCityMissing = AddressUpdateErrorV7.ScriptVariantCityMissing,
                scriptVariantDuplicateScriptCode = AddressUpdateErrorV7.ScriptVariantDuplicateScriptCode
            )
            is SiteNotInAddressLegalEntity ->
                ErrorInfoV7(
                    AddressUpdateErrorV7.SiteNotInLegalEntity,
                    "Site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is ScriptVariantCoverageStillNeeded ->
                ErrorInfoV7(
                    AddressUpdateErrorV7.ScriptVariantCoverageStillNeeded,
                    "Script code '${error.scriptCode}' must stay covered: business partner '${error.requiredByBpn}' " +
                            "is named in that script",
                    entityKey
                )
            is UnresolvableSite ->
                ErrorInfoV7(AddressUpdateErrorV7.SiteNotFound, "Site '${error.bpn}' not found", entityKey)
            is SiteMainAddressOmitted ->
                ErrorInfoV7(
                    AddressUpdateErrorV7.SiteMainAddressOmitted,
                    "Site '${error.siteBpn}' has this address as its main address and must stay among its sites",
                    entityKey
                )
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
        }

    fun toLegalEntityCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV7<LegalEntityCreateErrorV7> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateErrorV7.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityCreateErrorV7.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityCreateErrorV7.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateErrorV7.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateErrorV7.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityCreateErrorV7.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityCreateErrorV7.LegalAddressScriptVariantDuplicateScriptCode
        )

    fun toLegalEntityUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV7<LegalEntityUpdateErrorV7> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateErrorV7.LegalAddressRegionNotFound,
            countryNotFound = LegalEntityUpdateErrorV7.LegalAddressCountryNotFound,
            identifierNotFound = LegalEntityUpdateErrorV7.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateErrorV7.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateErrorV7.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityUpdateErrorV7.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityUpdateErrorV7.LegalAddressScriptVariantDuplicateScriptCode
        )

    fun toSiteCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV7<SiteCreateErrorV7> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateErrorV7.MainAddressRegionNotFound,
            countryNotFound = SiteCreateErrorV7.MainAddressCountryNotFound,
            identifierNotFound = SiteCreateErrorV7.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateErrorV7.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateErrorV7.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteCreateErrorV7.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteCreateErrorV7.MainAddressScriptVariantDuplicateScriptCode
        )

    fun toSiteUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfoV7<SiteUpdateErrorV7> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateErrorV7.MainAddressRegionNotFound,
            countryNotFound = SiteUpdateErrorV7.MainAddressCountryNotFound,
            identifierNotFound = SiteUpdateErrorV7.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateErrorV7.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateErrorV7.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteUpdateErrorV7.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteUpdateErrorV7.MainAddressScriptVariantDuplicateScriptCode
        )

    private fun <E : ErrorCodeV7> sharedErrorInfo(
        error: AddressContentParseError,
        entityKey: String?,
        regionNotFound: E,
        countryNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantCityMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfoV7<E> =
        when (error) {
            is AddressFieldParseError -> throw internalError(error)
            is AddressMetadataParseError -> when (error) {
                is AddressMetadataParseError.IdentifierTypeNotFound ->
                    ErrorInfoV7(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataParseError.PhysicalCountryNotFound ->
                    ErrorInfoV7(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataParseError.AlternativeCountryNotFound ->
                    ErrorInfoV7(countryNotFound, "Address country '${error.country}' does not exist", entityKey)
                is AddressMetadataParseError.PhysicalRegionNotFound ->
                    ErrorInfoV7(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.AlternativeRegionNotFound ->
                    ErrorInfoV7(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintParseError -> when (error) {
                is AddressConstraintParseError.IdentifiersTooMany ->
                    ErrorInfoV7(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintParseError.DuplicateIdentifier ->
                    ErrorInfoV7(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantParseError -> when (error) {
                is AddressScriptVariantParseError.PhysicalCityMissing ->
                    ErrorInfoV7(scriptVariantCityMissing, "Script variant ${error.index} has no city in its physical address", entityKey)
                is AddressScriptVariantParseError.AlternativeCityMissing ->
                    ErrorInfoV7(scriptVariantCityMissing, "Script variant ${error.index} has no city in its alternative address", entityKey)
                is AddressScriptVariantParseError.DuplicateScriptCode ->
                    ErrorInfoV7(scriptVariantDuplicateScriptCode, "Duplicate address script variant for script code '${error.scriptCode}'", entityKey)
            }
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected address validation error that has no client error code: $error")
}
