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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Updates logistic addresses identified by BPN (the "additional address" REST and task paths). This is the thin,
 * resolving layer on top of [AddressUpdateService]: it resolves the target BPN to its existing entity (yielding
 * `UnresolvableAddress` on a miss) and delegates content validation and the update to the lower service. Order-preserving
 * positional contract (see [ParseResult]).
 */
@Service
class AdditionalAddressUpdateService(
    private val addressUpdateService: AddressUpdateService,
    private val addressBpnParser: AddressBpnParser
) {

    fun parse(requests: List<AddressUpdateRequest>): List<ParseResult<AddressUpdateParsed, AddressUpdateParseError>> {
        val contentResults = addressUpdateService.parseContent(requests.map { it.content }, requests.map { it.addressBpn })
        val targetResults = addressBpnParser.parse(requests.map { it.addressBpn })

        return zipParseResults(contentResults, targetResults) { content, target ->
            AddressUpdateParsed(target, content.address, content.scriptVariants)
        }
    }

    @Transactional
    fun parseAndUpdate(requests: List<AddressUpdateRequest>): List<ParseResult<UpsertResult<LogisticAddressDb>, AddressUpdateParseError>> =
        addressUpdateService.parseAndUpdate(parse(requests))
}
