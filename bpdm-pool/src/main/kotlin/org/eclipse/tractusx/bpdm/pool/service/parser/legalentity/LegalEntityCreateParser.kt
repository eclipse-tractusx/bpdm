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

package org.eclipse.tractusx.bpdm.pool.service.parser.legalentity

import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.crossValidateParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service

/**
 * Validates legal-entity create requests: the header content with its identifier uniqueness, the legal address, and that
 * the legal address covers every script code the header names.
 */
@Service
class LegalEntityCreateParser(
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val addressContentParser: AddressContentParser,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator,
) {

    /**
     * Validates each request and reports either the validated legal entity or every problem found in that entry.
     */
    fun parse(requests: List<LegalEntityCreateRequest>): List<ParseResult<LegalEntityCreateParsed, LegalEntityCreateParseError>> {
        val headers = requests.map { it.content.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        val duplicateErrors = duplicateValidator.validate(headers, headers.map { null })
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        val legalAddresses = requests.map { it.content.legalAddress }
        val legalAddressResults = addressContentParser.parse(legalAddresses, legalAddresses.map { null })
        val coveredHeaderResults: List<ParseResult<LegalEntityHeaderParsed, LegalEntityCreateParseError>> =
            crossValidateParseResults(legalAddressResults, mergedHeaderResults) { legalAddress, header ->
                scriptVariantCoverageValidator.check(legalAddress.scriptCodes(), listOf(PartnerScriptCodes(bpn = null, header.scriptCodes())))
            }

        return zipParseResults(coveredHeaderResults, legalAddressResults) { header, legalAddress ->
            LegalEntityCreateParsed(LegalEntityContentParsed(header, legalAddress))
        }
    }
}
