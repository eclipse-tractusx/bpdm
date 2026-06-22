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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressUpdateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.AddressConstraintParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableSite
import org.eclipse.tractusx.bpdm.pool.model.AddressFieldParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressMetadataParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableAddress
import org.springframework.stereotype.Component

/**
 * Translates the address services' sealed parse errors into the version-specific [ErrorInfo] codes the
 * `/addresses` endpoints return. The `entityKey` (request index on create, BPN on update) is supplied by the
 * caller since it is a request-positioning concern, not part of the error.
 *
 * Two error cases have no equivalent in the public [AddressCreateError]/[AddressUpdateError] enums and are
 * treated as internal errors (throw → 500) rather than extending the API:
 *  - [AddressMetadataParseError.ScriptCodeNotFound]: the old REST path did not validate script codes and would
 *    NPE on an unknown one, so a 500 preserves that behaviour.
 *  - every [AddressFieldParseError]: unreachable here because the bounded Pool DTO already guarantees the
 *    presence/format these check; the throw asserts that invariant.
 *
 * The `when` blocks are deliberately exhaustive so a newly added parse error fails to compile here until it is
 * given a code (or an explicit internal-error decision).
 */
@Component
class AddressParseErrorMapper {

    fun toCreateErrorInfo(error: AddressCreateParseError, entityKey: String?): ErrorInfo<AddressCreateError> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfo(AddressCreateError.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is UnresolvableSite ->
                ErrorInfo(AddressCreateError.SiteNotFound, "Parent site '${error.bpn}' not found", entityKey)
            is AddressContentParseError -> sharedErrorInfo(
                error,
                entityKey,
                regionNotFound = AddressCreateError.RegionNotFound,
                identifierNotFound = AddressCreateError.IdentifierNotFound,
                duplicateIdentifier = AddressCreateError.AddressDuplicateIdentifier,
                identifiersTooMany = AddressCreateError.IdentifiersTooMany
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
                identifiersTooMany = AddressUpdateError.IdentifiersTooMany
            )
        }

    /**
     * For the legal address embedded in a legal-entity create: the content parser only ever produces
     * [AddressContentParseError] (there is no parent to resolve here), mapped to the `LegalAddress*` codes.
     */
    fun toLegalEntityCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<LegalEntityCreateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = LegalEntityCreateError.LegalAddressRegionNotFound,
            identifierNotFound = LegalEntityCreateError.LegalAddressIdentifierNotFound,
            duplicateIdentifier = LegalEntityCreateError.LegalAddressDuplicateIdentifier,
            identifiersTooMany = LegalEntityCreateError.LegalAddressIdentifiersTooMany
        )

    /**
     * For the main address embedded in a site create: the content parser only ever produces
     * [AddressContentParseError], mapped to the `MainAddress*` codes.
     */
    fun toSiteCreateErrorInfo(error: AddressContentParseError, entityKey: String?): ErrorInfo<SiteCreateError> =
        sharedErrorInfo(
            error,
            entityKey,
            regionNotFound = SiteCreateError.MainAddressRegionNotFound,
            identifierNotFound = SiteCreateError.MainAddressIdentifierNotFound,
            duplicateIdentifier = SiteCreateError.MainAddressDuplicateIdentifier,
            identifiersTooMany = SiteCreateError.MainAddressIdentifiersTooMany
        )

    private fun <E : ErrorCode> sharedErrorInfo(
        error: AddressContentParseError,
        entityKey: String?,
        regionNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
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
        }

    private fun internalError(error: AddressContentParseError) =
        BpdmValidationException("Unexpected address validation error that has no client error code: $error")
}
