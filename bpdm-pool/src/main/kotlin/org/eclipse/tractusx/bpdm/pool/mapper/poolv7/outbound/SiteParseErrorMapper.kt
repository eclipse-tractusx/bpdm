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

import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalAddressAlreadyMainAddress
import org.eclipse.tractusx.bpdm.pool.model.error.SiteContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableAddress
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.UnresolvableSite
import org.springframework.stereotype.Component

/**
 * Maps the site services' sealed parse errors to the `/sites` [ErrorInfo] codes (main-address errors delegated to
 * [AddressParseErrorMapper]). [SiteContentParseError] has no public code (name/confidence guaranteed by the bounded DTO;
 * unknown header script code previously NPE'd) and neither does [UnresolvableAddress] (its path is only invoked for an
 * address just resolved) — all internal errors. The `when`s are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class SiteParseErrorMapper(
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    fun toCreateErrorInfo(error: SiteCreateParseError, entityKey: String?): ErrorInfo<SiteCreateError> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfo(SiteCreateError.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is LegalAddressAlreadyMainAddress ->
                ErrorInfo(SiteCreateError.MainAddressDuplicateIdentifier, "Legal address already belongs to site '${error.bpnSite}'", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteCreateErrorInfo(error, entityKey)
            is UnresolvableAddress -> throw internalError(error)
            is SiteContentParseError -> throw internalError(error)
        }

    fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfo<SiteUpdateError> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfo(SiteUpdateError.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteUpdateErrorInfo(error, entityKey)
            is SiteContentParseError -> throw internalError(error)
        }

    private fun internalError(error: SiteCreateParseError) =
        BpdmValidationException("Unexpected site parse error (no public error code): $error")
}
