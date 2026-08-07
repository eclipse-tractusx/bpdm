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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.model.request.AddressSearchRequest
import org.springframework.stereotype.Component

/**
 * Creates address search criteria from the v6 API search requests.
 */
@Component
class AddressSearchRequestMapperV6 {

    /**
     * Combines the criteria a client sent with the Catena-X member restriction that the endpoint they sent them to
     * imposes.
     */
    fun toSearchRequest(searchRequest: AddressSearchRequestV6, isDataSpaceParticipant: Boolean?): AddressSearchRequest =
        AddressSearchRequest(
            addressBpns = searchRequest.addressBpns,
            siteBpns = searchRequest.siteBpns,
            legalEntityBpns = searchRequest.legalEntityBpns,
            name = searchRequest.name,
            isDataSpaceParticipant = isDataSpaceParticipant,
            excludesSiteAddresses = false
        )

    /**
     * Builds the criteria for listing the addresses that belong to a legal entity directly instead of through one of
     * its sites.
     */
    fun toDirectAddressesRequest(legalEntityBpn: String): AddressSearchRequest =
        AddressSearchRequest(
            addressBpns = emptyList(),
            siteBpns = emptyList(),
            legalEntityBpns = listOf(legalEntityBpn),
            name = null,
            isDataSpaceParticipant = null,
            excludesSiteAddresses = true
        )
}
