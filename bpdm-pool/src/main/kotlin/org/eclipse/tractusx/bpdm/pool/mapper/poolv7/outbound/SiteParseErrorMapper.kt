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

import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.*
import org.springframework.stereotype.Component

/**
 * Maps the site services' sealed parse errors to the `/sites` [ErrorInfoV7] codes, delegating main-address errors to
 * [AddressParseErrorMapper].
 *
 * An error the bounded DTO already rules out, or that this operation cannot reach, gets no public code and is thrown as
 * an internal error instead; script-variant content is client-nullable and therefore does get public codes. The `when`s
 * are exhaustive so a new error won't compile until it gets a code.
 */
@Component
class SiteParseErrorMapper(
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    fun toUpdateErrorInfo(error: SiteUpdateError, entityKey: String?): ErrorInfoV7<SiteUpdateErrorV7> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfoV7(SiteUpdateErrorV7.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentError -> addressParseErrorMapper.toSiteUpdateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is SiteContentError -> contentErrorInfo(
                error,
                entityKey,
                scriptVariantNameMissing = SiteUpdateErrorV7.ScriptVariantNameMissing,
                scriptVariantDuplicateScriptCode = SiteUpdateErrorV7.ScriptVariantDuplicateScriptCode
            )
        }

    private fun <E : ErrorCodeV7> contentErrorInfo(
        error: SiteContentError,
        entityKey: String?,
        scriptVariantNameMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfoV7<E> =
        when (error) {
            is SiteContentError.ScriptVariantNameMissing ->
                ErrorInfoV7(scriptVariantNameMissing, "Script variant ${error.index} has no site name", entityKey)
            is SiteContentError.ScriptVariantDuplicateScriptCode ->
                ErrorInfoV7(scriptVariantDuplicateScriptCode, "Duplicate site script variant for script code '${error.scriptCode}'", entityKey)
            is SiteContentError.NameMissing,
            is SiteContentError.ConfidenceCriteriaMissing,
            is SiteContentError.ScriptCodeNotFound -> throw internalError(error)
        }

    private fun internalError(error: SiteCreateError) =
        BpdmValidationException("Unexpected site parse error (no public error code): $error")
}
