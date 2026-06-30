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

import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalAddressAlreadyMainAddress
import org.eclipse.tractusx.bpdm.pool.model.SiteContentParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableSite
import org.springframework.stereotype.Component

/**
 * Translates the site services' sealed parse errors into the version-specific [ErrorInfo] codes the `/sites` endpoints
 * return. Parent/target resolution and the embedded main-address errors (delegated to [AddressParseErrorMapper], mapped
 * to the `MainAddress*` codes) cover all reachable cases.
 *
 * [SiteContentParseError] (site header) has no public `Site*Error` code: name/confidence are guaranteed present by the
 * bounded REST DTO (so those are unreachable), and an unknown header script code previously NPE'd to a 500. Either way it
 * is treated as an internal error. The `when` blocks are exhaustive so a newly added parse error fails to compile here
 * until it is given a code.
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
            is SiteContentParseError -> throw internalError(error)
        }

    fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfo<SiteUpdateError> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfo(SiteUpdateError.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteUpdateErrorInfo(error, entityKey)
            is SiteContentParseError -> throw internalError(error)
        }

    private fun internalError(error: SiteContentParseError) =
        BpdmValidationException("Unexpected site content parse error (no public error code): $error")
}
