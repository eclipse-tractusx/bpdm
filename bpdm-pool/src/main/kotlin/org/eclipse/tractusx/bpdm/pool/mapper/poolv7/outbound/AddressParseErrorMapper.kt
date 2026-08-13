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

import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the address services' sealed parse errors to the `/addresses` [ErrorInfo] codes.
 *
 * An error the bounded DTO already rules out, or that this operation cannot reach, gets no public code and is thrown as
 * an internal error instead. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class AddressParseErrorMapper {

    fun toCreateErrorInfo(error: AddressCreateParseError, entityKey: String?): ErrorInfo<AddressCreateError> =
        when (error) {
            is InvalidParentBpn ->
                ErrorInfo(AddressCreateError.BpnNotValid, "Parent '${error.bpn}' is not a valid BPNL/BPNS", entityKey)
            is UnresolvableLegalEntity ->
                ErrorInfo(AddressCreateError.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is UnresolvableSite ->
                ErrorInfo(AddressCreateError.SiteNotFound, "Parent site '${error.bpn}' not found", entityKey)
            is SiteNotInAddressLegalEntity ->
                ErrorInfo(
                    AddressCreateError.SiteNotInLegalEntity,
                    "Parent site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressCreateError.RegionNotFound,
                identifierNotFound = AddressCreateError.IdentifierNotFound,
                duplicateIdentifier = AddressCreateError.AddressDuplicateIdentifier,
                identifiersTooMany = AddressCreateError.IdentifiersTooMany,
                scriptVariantCityMissing = AddressCreateError.ScriptVariantCityMissing,
                scriptVariantDuplicateScriptCode = AddressCreateError.ScriptVariantDuplicateScriptCode
            )
        }

    fun toUpdateErrorInfo(error: AddressUpdateParseError, entityKey: String?): ErrorInfo<AddressUpdateError> =
        when (error) {
            is UnresolvableAddress ->
                ErrorInfo(AddressUpdateError.AddressNotFound, "Address '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressUpdateError.RegionNotFound,
                identifierNotFound = AddressUpdateError.IdentifierNotFound,
                duplicateIdentifier = AddressUpdateError.AddressDuplicateIdentifier,
                identifiersTooMany = AddressUpdateError.IdentifiersTooMany,
                scriptVariantCityMissing = AddressUpdateError.ScriptVariantCityMissing,
                scriptVariantDuplicateScriptCode = AddressUpdateError.ScriptVariantDuplicateScriptCode
            )
            is SiteNotInAddressLegalEntity ->
                ErrorInfo(
                    AddressUpdateError.SiteNotInLegalEntity,
                    "Site '${error.siteBpn}' does not belong to legal entity '${error.legalEntityBpn}'",
                    entityKey
                )
            is ScriptVariantCoverageStillNeeded ->
                ErrorInfo(
                    AddressUpdateError.ScriptVariantCoverageStillNeeded,
                    "Script code '${error.scriptCode}' must stay covered: business partner '${error.requiredByBpn}' " +
                            "is named in that script",
                    entityKey
                )
            is UnresolvableSite,
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
        }

    fun toLegalEntityCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<LegalEntityCreateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateError.LegalAddressRegionNotFound,
            identifierNotFound = LegalEntityCreateError.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateError.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateError.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityCreateError.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityCreateError.LegalAddressScriptVariantDuplicateScriptCode
        )

    fun toLegalEntityUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<LegalEntityUpdateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityUpdateError.LegalAddressRegionNotFound,
            identifierNotFound = LegalEntityUpdateError.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityUpdateError.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityUpdateError.LegalAddressIdentifiersTooMany,
            scriptVariantCityMissing = LegalEntityUpdateError.LegalAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = LegalEntityUpdateError.LegalAddressScriptVariantDuplicateScriptCode
        )

    fun toSiteCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<SiteCreateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateError.MainAddressRegionNotFound,
            identifierNotFound = SiteCreateError.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateError.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateError.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteCreateError.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteCreateError.MainAddressScriptVariantDuplicateScriptCode
        )

    fun toSiteUpdateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<SiteUpdateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteUpdateError.MainAddressRegionNotFound,
            identifierNotFound = SiteUpdateError.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteUpdateError.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteUpdateError.MainAddressIdentifiersTooMany,
            scriptVariantCityMissing = SiteUpdateError.MainAddressScriptVariantCityMissing,
            scriptVariantDuplicateScriptCode = SiteUpdateError.MainAddressScriptVariantDuplicateScriptCode
        )

    private fun <E : ErrorCode> sharedErrorInfo(
        error: AddressContentParseError,
        entityKey: String?,
        regionNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantCityMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfo<E> =
        when (error) {
            is AddressFieldParseError -> throw internalError(error)
            is AddressMetadataParseError -> when (error) {
                is AddressMetadataParseError.IdentifierTypeNotFound ->
                    ErrorInfo(identifierNotFound, "Address Identifier Type '${error.type}' does not exist", entityKey)
                is AddressMetadataParseError.PhysicalRegionNotFound ->
                    ErrorInfo(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.AlternativeRegionNotFound ->
                    ErrorInfo(regionNotFound, "Address administrative area level1 '${error.regionCode}' does not exist", entityKey)
                is AddressMetadataParseError.ScriptCodeNotFound -> throw internalError(error)
            }
            is AddressConstraintParseError -> when (error) {
                is AddressConstraintParseError.IdentifiersTooMany ->
                    ErrorInfo(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
                is AddressConstraintParseError.DuplicateIdentifier ->
                    ErrorInfo(duplicateIdentifier, "Duplicate Address Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            }
            is AddressScriptVariantParseError -> when (error) {
                is AddressScriptVariantParseError.PhysicalCityMissing ->
                    ErrorInfo(scriptVariantCityMissing, "Script variant ${error.index} has no city in its physical address", entityKey)
                is AddressScriptVariantParseError.AlternativeCityMissing ->
                    ErrorInfo(scriptVariantCityMissing, "Script variant ${error.index} has no city in its alternative address", entityKey)
                is AddressScriptVariantParseError.DuplicateScriptCode ->
                    ErrorInfo(scriptVariantDuplicateScriptCode, "Duplicate address script variant for script code '${error.scriptCode}'", entityKey)
            }
        }

    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected address validation error that has no client error code: $error")
}
