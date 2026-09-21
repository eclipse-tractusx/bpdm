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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityContentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.springframework.stereotype.Service

/**
 * Validates the content one legal entity states — its header with its identifier uniqueness, and its legal address.
 */
@Service
class LegalEntityContentParser(
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val addressContentParser: AddressContentParser
) {

    /**
     * Validates each content and reports either the validated content or every problem found in that entry.
     * [legalEntityBpns] and [legalAddressBpns] are positional with [contents]: null for a create, the record's own BPN
     * for an update, so an update may re-submit its own existing identifiers.
     */
    fun parse(
        contents: List<LegalEntityContentRequest>,
        legalEntityBpns: List<String?>,
        legalAddressBpns: List<String?>
    ): List<ParseResult<LegalEntityContentParsed, LegalEntityContentParseError>> {
        val headers = contents.map { it.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        val duplicateErrors = duplicateValidator.validate(headers, legalEntityBpns)
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        val legalAddressResults = addressContentParser.parse(contents.map { it.legalAddress }, legalAddressBpns)

        return zipParseResults(mergedHeaderResults, legalAddressResults, ::LegalEntityContentParsed)
    }
}
