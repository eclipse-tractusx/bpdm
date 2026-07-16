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

import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressContentRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.service.LogisticAddressRequestParser
import org.springframework.stereotype.Service

/**
 * The single owner of address-content validation, shared by every address create and update path. Combines the pure
 * content parse ([LogisticAddressRequestParser]) with the identity-aware identifier uniqueness check
 * ([AddressIdentifierDuplicateValidator]) into one order-preserving, per-entry result.
 *
 * [ownerBpns] is positional with [contents]: `null` for create (a new address owns no identifiers yet), the address's
 * own BPN for update (so it may legitimately re-submit its own existing identifiers). Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class AddressContentParser(
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator
) {

    fun parse(contents: List<AddressContentRequest>, ownerBpns: List<String?>): List<ParseResult<AddressContentParsed, AddressContentParseError>> {
        val contentResults = addressRequestParser.parse(contents)
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns)
        return contentResults.mapIndexed { index, result -> result.combine(duplicateErrors[index]) { it } }
    }
}
