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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the site services' sealed parse errors to the v6 `/sites` [ErrorInfoV6] codes, delegating main-address errors to
 * [AddressParseErrorMapperV6].
 *
 * The v6 error enums are frozen and predate script variants, so every script-variant error is thrown as an internal
 * error instead of getting a public code. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class SiteParseErrorMapperV6(
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6
) {

    fun toCreateErrorInfo(error: SiteCreateParseError, entityKey: String?): ErrorInfoV6<SiteCreateErrorV6> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV6(SiteCreateErrorV6.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is LegalAddressAlreadyMainAddress ->
                ErrorInfoV6(
                    SiteCreateErrorV6.MainAddressDuplicateIdentifier,
                    "Legal address already belongs to site '${error.bpnSite}'",
                    entityKey
                )
            is AddressContentParseError -> addressParseErrorMapperV6.toSiteCreateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress,
            is UnresolvableAddress,
            is ScriptVariantCoverageStillNeeded -> throw internalError(error)
            is SiteContentParseError -> throw internalError(error)
        }

    fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfoV6<SiteUpdateErrorV6> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfoV6(SiteUpdateErrorV6.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapperV6.toSiteUpdateErrorInfo(error, entityKey)
            // Reachable over v6: a v6 write sends no script variants, so it can drop coverage another business partner
            // still needs. The frozen v6 enum has no code for it, so the client gets an internal error.
            is ScriptVariantCoverageStillNeeded -> throw internalError(error)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is SiteContentParseError -> throw internalError(error)
        }

    private fun internalError(error: SiteCreateParseError) =
        BpdmValidationException("Unexpected site parse error (no v6 client error code): $error")
}
