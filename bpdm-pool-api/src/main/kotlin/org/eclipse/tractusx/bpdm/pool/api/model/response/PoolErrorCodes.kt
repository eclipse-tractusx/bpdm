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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Marks an enum of the error codes one endpoint can answer with.
 *
 * Each endpoint gets an enum of its own so that the generated Swagger schema lists exactly the codes that endpoint can
 * return.
 */
interface ErrorCode

@Schema(description = "LegalEntityCreateError")
enum class LegalEntityCreateError : ErrorCode {
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
    LegalAddressIdentifierNotFound,
    LegalAddressDuplicateIdentifier,
    LegalEntityIdentifiersTooMany,
    LegalAddressIdentifiersTooMany,
    ScriptVariantLegalNameMissing,
    ScriptVariantDuplicateScriptCode,
    LegalAddressScriptVariantCityMissing,
    LegalAddressScriptVariantDuplicateScriptCode
}

@Schema(description = "LegalEntityUpdateError")
enum class LegalEntityUpdateError : ErrorCode {
    LegalEntityNotFound,
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
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

@Schema(description = "SiteCreateError")
enum class SiteCreateError : ErrorCode {
    LegalEntityNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany,
    ScriptVariantNameMissing,
    ScriptVariantDuplicateScriptCode,
    MainAddressScriptVariantCityMissing,
    MainAddressScriptVariantDuplicateScriptCode,
    ScriptVariantNotCoveredByMainAddress
}

@Schema(description = "SiteUpdateError")
enum class SiteUpdateError : ErrorCode {
    SiteNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany,
    ScriptVariantNameMissing,
    ScriptVariantDuplicateScriptCode,
    MainAddressScriptVariantCityMissing,
    MainAddressScriptVariantDuplicateScriptCode
}

@Schema(description = "AddressCreateError")
enum class AddressCreateError : ErrorCode {
    BpnNotValid,
    SiteNotFound,
    SiteNotInLegalEntity,
    LegalEntityNotFound,
    RegionNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany,
    ScriptVariantCityMissing,
    ScriptVariantDuplicateScriptCode
}

@Schema(description = "AddressUpdateError")
enum class AddressUpdateError : ErrorCode {
    AddressNotFound,
    SiteNotFound,
    SiteNotInLegalEntity,
    SiteMainAddressOmitted,
    RegionNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany,
    ScriptVariantCityMissing,
    ScriptVariantDuplicateScriptCode
}