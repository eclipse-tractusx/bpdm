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

package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressSearchParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.bpn.BpnFilterParser
import org.springframework.stereotype.Service

/**
 * Turns loose address search criteria into the normalized form the search operation queries with.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: no search criterion
 * can be rejected — an unknown or malformed filter value matches nothing — so there is no failure to report.
 */
@Service
class AddressSearchParser(
    private val bpnFilterParser: BpnFilterParser
) {

    /**
     * Normalizes the criteria by dropping blank filter values and reading BPNs case-insensitively.
     */
    fun parse(request: AddressSearchRequest): AddressSearchParsed =
        AddressSearchParsed(
            addressBpns = bpnFilterParser.parse(request.addressBpns),
            siteBpns = bpnFilterParser.parse(request.siteBpns),
            legalEntityBpns = bpnFilterParser.parse(request.legalEntityBpns),
            name = request.name?.takeIf { it.isNotBlank() },
            isDataSpaceParticipant = request.isDataSpaceParticipant,
            excludesSiteAddresses = request.excludesSiteAddresses
        )
}
