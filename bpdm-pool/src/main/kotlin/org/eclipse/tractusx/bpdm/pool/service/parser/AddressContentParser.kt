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
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.springframework.stereotype.Service

/**
 * Validates the descriptive content of an address — the shared entry point reused for standalone addresses,
 * legal-entity legal addresses, and site main addresses.
 */
@Service
class AddressContentParser(
    private val addressRequestParser: AddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator
) {

    /**
     * Validates each address content, including its identifier uniqueness, and reports either the validated address or
     * every problem found in that entry. [ownerBpns] is positional with [contents]: null for a create, the address's own
     * BPN for an update, so an update may re-submit its own existing identifiers.
     */
    fun parse(contents: List<LogisticAddressRequest>, ownerBpns: List<String?>): List<ParseResult<LogisticAddressParsed, AddressContentParseError>> {
        val contentResults = addressRequestParser.parse(contents)
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns)
        return contentResults.mapIndexed { index, result -> result.combine(duplicateErrors[index]) { it } }
    }
}
