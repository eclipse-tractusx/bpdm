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

import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.model.request.DataSpaceParticipantSearchRequest
import org.eclipse.tractusx.bpdm.pool.model.request.DataSpaceParticipantUpdateRequest
import org.springframework.stereotype.Component

/**
 * Creates data space participation requests from the v6 API Catena-X membership requests, which name the same concept.
 */
@Component
class CxMembershipRequestMapperV6 {

    /**
     * Returns the search request holding the criteria a client sent.
     */
    fun toSearchRequest(searchRequest: CxMembershipSearchRequestV6): DataSpaceParticipantSearchRequest =
        DataSpaceParticipantSearchRequest(
            legalEntityBpns = searchRequest.bpnLs,
            isDataSpaceParticipant = searchRequest.isCatenaXMember
        )

    /**
     * Returns one update request per membership a client sent, in the order they were sent.
     */
    fun toUpdateRequests(updateRequest: CxMembershipUpdateRequestV6): List<DataSpaceParticipantUpdateRequest> =
        updateRequest.memberships.map {
            DataSpaceParticipantUpdateRequest(legalEntityBpn = it.bpnL, isDataSpaceParticipant = it.isCatenaXMember)
        }
}
