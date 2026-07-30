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

import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorCode
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteUpdateError
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the site services' sealed parse errors to the `/sites` [ErrorInfo] codes (main-address errors delegated to
 * [AddressParseErrorMapper]). The site's own name/confidence errors have no public code (guaranteed by the bounded DTO),
 * an unknown header script code previously NPE'd, and [UnresolvableAddress] is only invoked for an address just resolved
 * — all internal errors. Script-variant content is client-nullable and therefore does get public codes. The `when`s are
 * exhaustive so a new error won't compile until it gets a code.
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
            is ScriptVariantWithoutAddressRendering ->
                ErrorInfo(
                    SiteCreateError.ScriptVariantWithoutMainAddressRendering,
                    "Script code '${error.scriptCode}' is not rendered by the site's main address",
                    entityKey
                )
            is UnresolvableAddress -> throw internalError(error)
            is SiteContentParseError -> contentErrorInfo(
                error,
                entityKey,
                scriptVariantNameMissing = SiteCreateError.ScriptVariantNameMissing,
                scriptVariantDuplicateScriptCode = SiteCreateError.ScriptVariantDuplicateScriptCode
            )
        }

    fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfo<SiteUpdateError> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfo(SiteUpdateError.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteUpdateErrorInfo(error, entityKey)
            is SiteContentParseError -> contentErrorInfo(
                error,
                entityKey,
                scriptVariantNameMissing = SiteUpdateError.ScriptVariantNameMissing,
                scriptVariantDuplicateScriptCode = SiteUpdateError.ScriptVariantDuplicateScriptCode
            )
        }

    private fun <E : ErrorCode> contentErrorInfo(
        error: SiteContentParseError,
        entityKey: String?,
        scriptVariantNameMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfo<E> =
        when (error) {
            is SiteContentParseError.ScriptVariantNameMissing ->
                ErrorInfo(scriptVariantNameMissing, "Script variant ${error.index} has no site name", entityKey)
            is SiteContentParseError.ScriptVariantDuplicateScriptCode ->
                ErrorInfo(scriptVariantDuplicateScriptCode, "Duplicate site script variant for script code '${error.scriptCode}'", entityKey)
            is SiteContentParseError.NameMissing,
            is SiteContentParseError.ConfidenceCriteriaMissing,
            is SiteContentParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: SiteCreateParseError) =
        BpdmValidationException("Unexpected site parse error (no public error code): $error")
}
