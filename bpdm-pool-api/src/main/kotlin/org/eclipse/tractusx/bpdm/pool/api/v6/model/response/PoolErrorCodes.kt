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

package org.eclipse.tractusx.bpdm.pool.api.v6.model.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * For each endpoint a separate enum class is defined extending this marker interface.
 * We need separate enum classes in order to get the correct error codes for each endpoint in the Swagger schema.
 */
interface ErrorCodeV6

@Schema(description = "LegalEntityCreateErrorV6", deprecated = true)
enum class LegalEntityCreateErrorV6 : ErrorCodeV6 {
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
    LegalAddressIdentifierNotFound,
    LegalAddressDuplicateIdentifier,
    LegalEntityIdentifiersTooMany,
    LegalAddressIdentifiersTooMany
}

@Schema(description = "LegalEntityUpdateErrorV6", deprecated = true)
enum class LegalEntityUpdateErrorV6 : ErrorCodeV6 {
    LegalEntityNotFound,
    LegalEntityDuplicateIdentifier,
    LegalFormNotFound,
    LegalEntityIdentifierNotFound,
    LegalAddressRegionNotFound,
    LegalAddressIdentifierNotFound,
    LegalAddressDuplicateIdentifier,
    LegalEntityIdentifiersTooMany,
    LegalAddressIdentifiersTooMany
}

@Schema(description = "SiteCreateErrorV6", deprecated = true)
enum class SiteCreateErrorV6 : ErrorCodeV6 {
    LegalEntityNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany
}

@Schema(description = "SiteUpdateErrorV6", deprecated = true)
enum class SiteUpdateErrorV6 : ErrorCodeV6 {
    SiteNotFound,
    MainAddressIdentifierNotFound,
    MainAddressRegionNotFound,
    MainAddressDuplicateIdentifier,
    MainAddressIdentifiersTooMany
}

@Schema(description = "AddressCreateErrorV6", deprecated = true)
enum class AddressCreateErrorV6 : ErrorCodeV6 {
    BpnNotValid,
    SiteNotFound,
    SiteNotInLegalEntity,
    LegalEntityNotFound,
    RegionNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany
}

@Schema(description = "AddressUpdateErrorV6", deprecated = true)
enum class AddressUpdateErrorV6 : ErrorCodeV6 {
    AddressNotFound,
    SiteNotInLegalEntity,
    RegionNotFound,
    IdentifierNotFound,
    AddressDuplicateIdentifier,
    IdentifiersTooMany
}