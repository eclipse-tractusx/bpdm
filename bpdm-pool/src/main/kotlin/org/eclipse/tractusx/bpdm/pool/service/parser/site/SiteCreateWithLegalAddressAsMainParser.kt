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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.error.LegalAddressAlreadyMainAddress
import org.eclipse.tractusx.bpdm.pool.model.error.SiteCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteCreateWithLegalAddressAsMainRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.springframework.stereotype.Service

/**
 * Validates site-create requests that take the parent legal entity's legal address as the site main address, and, where a
 * request claims that address exclusively, that no site has it as main address yet.
 *
 * It yields the referenced-main-address model: this path is that case with the reference fixed to the parent's legal
 * address, so it needs no operation of its own.
 */
@Service
class SiteCreateWithLegalAddressAsMainParser(
    private val siteHeaderParser: SiteHeaderParser,
    private val legalEntityBpnParser: LegalEntityBpnParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator,
) {

    /**
     * Validates each request and reports either the validated site on its parent's legal address or every problem found
     * in that entry.
     */
    fun parse(
        requests: List<SiteCreateWithLegalAddressAsMainRaw>
    ): List<ParseResult<SiteCreateWithReferencedAddressAsMainParsed, SiteCreateParseError>> {
        val headerResults = siteHeaderParser.parse(requests.map { it.header })
        val legalEntityResults: List<ParseResult<LegalEntityDb, SiteCreateParseError>> =
            legalEntityBpnParser.parse(requests.map { it.legalEntityBpn }).zip(requests) { legalEntityResult, request ->
                if (legalEntityResult is ParseResult.Success && request.exclusiveLegalAddress)
                    legalEntityResult.combine(legalAddressClaimErrors(legalEntityResult.parsed)) { it }
                else
                    legalEntityResult
            }
        val coveredHeaderResults: List<ParseResult<SiteHeaderParsed, SiteCreateParseError>> =
            crossValidateParseResults(legalEntityResults, headerResults) { legalEntity, header ->
                scriptVariantCoverageValidator.check(legalEntity.legalAddress.scriptCodes(), header.scriptCodes())
            }

        return zipParseResults(legalEntityResults, coveredHeaderResults) { legalEntity, header ->
            SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, header, mainAddressContent = null)
        }
    }

    private fun legalAddressClaimErrors(legalEntity: LegalEntityDb): List<SiteCreateParseError> =
        listOfNotNull(legalEntity.legalAddress.sites.firstOrNull()?.let { LegalAddressAlreadyMainAddress(it.bpn) })
}
