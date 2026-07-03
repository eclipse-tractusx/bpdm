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

import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateUntypedParentRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.chainParseResults
import org.springframework.stereotype.Service

/**
 * Parses address-create requests carrying a single, *untyped* parent BPN (the V7 "additional address" REST path) into the
 * resolved [AddressCreateParsed] command. Resolves the BPN into the explicit (legalEntity, site) parents via
 * [AddressParentResolutionParser] — reporting the precise `InvalidParentBpn`/`UnresolvableLegalEntity`/`UnresolvableSite`
 * errors the typed stage cannot distinguish — then delegates content validation to [TypedParentAddressCreateParser].
 * Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class UntypedParentAddressCreateParser(
    private val addressParentResolutionParser: AddressParentResolutionParser,
    private val typedParentAddressCreateParser: TypedParentAddressCreateParser
) {

    fun parse(requests: List<AddressCreateUntypedParentRequest>): List<ParseResult<AddressCreateParsed, AddressCreateParseError>> =
        chainParseResults(addressParentResolutionParser.parse(requests)) { typed -> typedParentAddressCreateParser.parse(typed) }
}
