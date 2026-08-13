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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CxMembershipDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound.CxMembershipRequestMapperV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.CxMembershipResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.service.operation.participation.DataSpaceParticipantSearchService
import org.eclipse.tractusx.bpdm.pool.service.parser.DataSpaceParticipantSearchParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "search Catena-X memberships" operation, using the v6 request/response shapes.
 */
@Service
class CxMembershipSearchApplicationV6Service(
    private val dataSpaceParticipantSearchParser: DataSpaceParticipantSearchParser,
    private val dataSpaceParticipantSearchService: DataSpaceParticipantSearchService,
    private val cxMembershipRequestMapperV6: CxMembershipRequestMapperV6,
    private val cxMembershipResponseMapperV6: CxMembershipResponseMapperV6
) {

    /**
     * Returns the requested page of Catena-X memberships matching the given criteria.
     */
    @Transactional(readOnly = true)
    fun searchMemberships(
        searchRequest: CxMembershipSearchRequestV6,
        paginationRequest: PaginationRequest
    ): PageDto<CxMembershipDtoV6> {
        val criteria = dataSpaceParticipantSearchParser.parse(cxMembershipRequestMapperV6.toSearchRequest(searchRequest))

        return dataSpaceParticipantSearchService.search(criteria, paginationRequest.toPageRequest())
            .toPageDto { cxMembershipResponseMapperV6.toMembership(it) }
    }
}
