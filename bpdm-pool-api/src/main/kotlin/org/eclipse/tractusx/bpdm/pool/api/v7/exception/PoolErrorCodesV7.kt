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

package org.eclipse.tractusx.bpdm.pool.api.v7.exception

import io.swagger.v3.oas.annotations.media.Schema

/**
 * For each endpoint a separate enum class is defined extending this marker interface.
 * We need separate enum classes in order to get the correct error codes for each endpoint in the Swagger schema.
 */
interface ErrorCodeV7

@Schema(description = "LegalEntityCreateErrorV7")
enum class LegalEntityCreateErrorV7 : ErrorCodeV7 {
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
    LegalAddressCountryNotFound,
    LegalAddressIdentifierNotFound,
    LegalAddressDuplicateIdentifier,
    LegalEntityIdentifiersTooMany,
    LegalAddressIdentifiersTooMany,
    ScriptVariantLegalNameMissing,
    ScriptVariantDuplicateScriptCode,
    LegalAddressScriptVariantCityMissing,
    LegalAddressScriptVariantDuplicateScriptCode
}

@Schema(description = "LegalEntityUpdateErrorV7")
enum class LegalEntityUpdateErrorV7 : ErrorCodeV7 {
    LegalEntityNotFound,
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
    LegalAddressCountryNotFound,
    LegalAddressIdentifierNotFound,
    LegalAddressDuplicateIdentifier,
    LegalEntityIdentifiersTooMany,
    LegalAddressIdentifiersTooMany,
    MultipleUltimateOwnersInHierarchy,
    AlternativeHeadquarterCannotOwnUltimately,
    ScriptVariantLegalNameMissing,
    ScriptVariantDuplicateScriptCode,
    LegalAddressScriptVariantCityMissing,
    LegalAddressScriptVariantDuplicateScriptCode
}

@Schema(description = "SiteCreateErrorV7")
enum class SiteCreateErrorV7 : ErrorCodeV7 {
    LegalEntityNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressCountryNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany,
    ScriptVariantNameMissing,
    ScriptVariantDuplicateScriptCode,
    MainAddressScriptVariantCityMissing,
    MainAddressScriptVariantDuplicateScriptCode,
    ScriptVariantNotCoveredByMainAddress
}

@Schema(description = "SiteUpdateErrorV7")
enum class SiteUpdateErrorV7 : ErrorCodeV7 {
    SiteNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressCountryNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany,
    ScriptVariantNameMissing,
    ScriptVariantDuplicateScriptCode,
    MainAddressScriptVariantCityMissing,
    MainAddressScriptVariantDuplicateScriptCode
}

@Schema(description = "AddressCreateErrorV7")
enum class AddressCreateErrorV7 : ErrorCodeV7 {
    BpnNotValid,
    SiteNotFound,
    SiteNotInLegalEntity,
    LegalEntityNotFound,
    RegionNotFound,
    CountryNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany,
    ScriptVariantCityMissing,
    ScriptVariantDuplicateScriptCode
}

@Schema(description = "AddressUpdateErrorV7")
enum class AddressUpdateErrorV7 : ErrorCodeV7 {
    AddressNotFound,
    SiteNotFound,
    SiteNotInLegalEntity,
    SiteMainAddressOmitted,
    RegionNotFound,
    CountryNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany,
    ScriptVariantCityMissing,
    ScriptVariantDuplicateScriptCode
}