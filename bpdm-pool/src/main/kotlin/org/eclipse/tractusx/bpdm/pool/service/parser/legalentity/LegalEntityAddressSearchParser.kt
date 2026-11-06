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
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityAddressSearchError.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LegalEntityAddressSearchRaw
import org.springframework.stereotype.Service

/**
 * Resolves the legal entity whose addresses are being listed into the criteria the address search operation queries
 * with.
 */
@Service
class LegalEntityAddressSearchParser(
    private val legalEntityBpnParser: LegalEntityBpnParser
) {

    /**
     * Returns criteria matching every address of the requested legal entity that belongs to none of its sites, failing
     * when no legal entity carries its BPN so that an unknown legal entity is distinguishable from one without such
     * addresses.
     */
    fun parse(request: LegalEntityAddressSearchRaw): ParseResult<AddressSearchParsed, LegalEntityAddressSearchError> =
        when (val result = legalEntityBpnParser.parse(listOf(request.legalEntityBpn)).single()) {
            is ParseResult.Failure -> ParseResult.ofSingleFailure(LegalEntityNotFound(request.legalEntityBpn))
            is ParseResult.Success -> ParseResult.Success(
                AddressSearchParsed(
                    addressBpns = emptyList(),
                    siteBpns = emptyList(),
                    legalEntityBpns = listOf(result.parsed.bpn),
                    name = null,
                    isDataSpaceParticipant = null,
                    excludesSiteAddresses = true
                )
            )
        }
}
