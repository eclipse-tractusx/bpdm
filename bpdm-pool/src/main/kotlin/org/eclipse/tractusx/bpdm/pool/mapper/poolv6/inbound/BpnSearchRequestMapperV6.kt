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

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.BpnRequestIdentifierSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.IdentifiersSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.model.request.BpnIdentifierSearchRequest
import org.eclipse.tractusx.bpdm.pool.model.request.BpnRequestIdentifierSearchRequest
import org.springframework.stereotype.Component

/**
 * Creates BPN search criteria from the v6 API search requests.
 */
@Component
class BpnSearchRequestMapperV6 {

    /**
     * Returns the criteria to search BPNs by the identifiers the client sent.
     */
    fun toIdentifierSearchRequest(searchRequest: IdentifiersSearchRequestV6): BpnIdentifierSearchRequest =
        BpnIdentifierSearchRequest(
            businessPartnerType = IdentifierBusinessPartnerType.valueOf(searchRequest.businessPartnerType.name),
            identifierTypeKey = searchRequest.idType,
            identifierValues = searchRequest.idValues
        )

    /**
     * Returns the criteria to search BPNs by the request identifiers the client sent.
     */
    fun toRequestIdentifierSearchRequest(searchRequest: BpnRequestIdentifierSearchRequestV6): BpnRequestIdentifierSearchRequest =
        BpnRequestIdentifierSearchRequest(requestedIdentifiers = searchRequest.requestedIdentifiers)
}
