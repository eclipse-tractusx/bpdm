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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.UnresolvableAddress
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.springframework.stereotype.Service

/**
 * Resolves address BPNs to their existing entities, batched and order-preserving (see [ParseResult]): an unresolvable
 * BPN yields an [UnresolvableAddress] for that entry. Single responsibility so any service that
 * needs to look up an address by BPN (e.g. an update target) can reuse it and combine its result with other parsers via
 * `zipParseResults`.
 */
@Service
class AddressBpnParser(
    private val logisticAddressRepository: LogisticAddressRepository,
) {

    fun parse(addressBpns: List<String>): List<ParseResult<LogisticAddressDb, UnresolvableAddress>> {
        val addressesByBpn = logisticAddressRepository
            .findDistinctByBpnIn(addressBpns.toSet())
            .associateBy { it.bpn }

        return addressBpns.map { bpn ->
            when (val address = addressesByBpn[bpn]) {
                null -> ParseResult.ofSingleFailure(UnresolvableAddress(bpn))
                else -> ParseResult.Success(address)
            }
        }
    }
}
