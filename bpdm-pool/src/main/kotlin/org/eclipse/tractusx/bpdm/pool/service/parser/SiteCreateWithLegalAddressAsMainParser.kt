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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithLegalAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service

/**
 * Validates site-create requests that take the parent legal entity's legal address as the site main address.
 *
 * It yields the referenced-main-address model: this path is that case with the reference fixed to the parent's legal
 * address, so it needs no operation of its own.
 */
@Service
class SiteCreateWithLegalAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val renderingValidator: ScriptVariantRenderingValidator
) {

    /**
     * Validates each request and reports either the validated site on its parent's legal address or every problem found
     * in that entry.
     */
    fun parse(
        requests: List<SiteCreateWithLegalAddressAsMainRequest>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val legalEntityResults = legalEntityBpnParser.parse(requests.map { it.legalEntityBpn })
        val renderedHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(legalEntityResults, headerResults) { legalEntity, header ->
                renderingValidator.check(
                    header.scriptVariants.map { it.scriptCode.technicalKey },
                    legalEntity.legalAddress.scriptVariants.map { it.scriptCode.technicalKey }
                )
            }

        return zipParseResults(legalEntityResults, renderedHeaderResults) { legalEntity, header ->
            SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, header)
        }
    }
}
