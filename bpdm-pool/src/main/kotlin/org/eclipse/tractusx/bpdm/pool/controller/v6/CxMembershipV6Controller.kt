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

package org.eclipse.tractusx.bpdm.pool.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolCxMembershipV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.participant.CxMembershipV6Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv6.ApiV6ParticipantMapper
import org.eclipse.tractusx.bpdm.pool.model.output.participation.DataSpaceParticipantUpdateResult
import org.eclipse.tractusx.bpdm.pool.service.application.DataSpaceParticipantSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.DataSpaceParticipantUpdateApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("CxMembershipControllerLegacy")
class CxMembershipV6Controller(
    private val dataSpaceParticipantSearchApplicationService: DataSpaceParticipantSearchApplicationService,
    private val dataSpaceParticipantUpdateApplicationService: DataSpaceParticipantUpdateApplicationService,
    private val apiV6InboundMapper: ApiV6InboundMapper,
    private val apiV6ParticipantMapper: ApiV6ParticipantMapper
): PoolCxMembershipV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun get(searchRequest: CxMembershipSearchV6Request, paginationRequest: PaginationRequest): PageDto<CxMembershipV6Dto> {
        val request = apiV6InboundMapper.toDataSpaceParticipantSearchRaw(searchRequest)
        return dataSpaceParticipantSearchApplicationService.search(request, paginationRequest.toPageRequest())
            .toPageDto(apiV6ParticipantMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun put(updateRequest: CxMembershipUpdateV6Request) {
        val requests = apiV6InboundMapper.toDataSpaceParticipantUpdateRaws(updateRequest)
        when (val result = dataSpaceParticipantUpdateApplicationService.update(requests)) {
            is DataSpaceParticipantUpdateResult.Updated -> Unit
            is DataSpaceParticipantUpdateResult.Rejected -> throw apiV6ParticipantMapper.toUpdateException(result.errors)
        }
    }
}
