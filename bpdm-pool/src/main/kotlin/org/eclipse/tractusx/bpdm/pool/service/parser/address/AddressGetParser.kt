/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressGetError.AddressNotFound
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressGetRaw
import org.springframework.stereotype.Service

/**
 * Resolves an address fetch request to the address it names.
 */
@Service
class AddressGetParser(
    private val addressBpnParser: AddressBpnParser
) {

    /**
     * Resolves the requested BPN to its address, failing when no address carries it.
     */
    fun parse(request: AddressGetRaw): ParseResult<LogisticAddressDb, AddressGetError> =
        when (val result = addressBpnParser.parse(listOf(request.bpn)).single()) {
            is ParseResult.Success -> result
            is ParseResult.Failure -> ParseResult.ofSingleFailure(AddressNotFound(request.bpn))
        }
}
