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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolDataSpaceParticipantsV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.participant.DataSpaceParticipantV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7ParticipantMapper
import org.eclipse.tractusx.bpdm.pool.model.output.participation.DataSpaceParticipantUpdateResult
import org.eclipse.tractusx.bpdm.pool.service.application.DataSpaceParticipantSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.DataSpaceParticipantUpdateApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class DataSpaceParticipantsController(
    private val dataSpaceParticipantSearchApplicationService: DataSpaceParticipantSearchApplicationService,
    private val dataSpaceParticipantUpdateApplicationService: DataSpaceParticipantUpdateApplicationService,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7ParticipantMapper: ApiV7ParticipantMapper
): PoolDataSpaceParticipantsV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun get(searchRequest: DataSpaceParticipantSearchV7Request, paginationRequest: PaginationRequest): PageDto<DataSpaceParticipantV7Dto> {
        val request = apiV7InboundMapper.toDataSpaceParticipantSearchRaw(searchRequest)
        return dataSpaceParticipantSearchApplicationService.search(request, paginationRequest.toPageRequest())
            .toPageDto(apiV7ParticipantMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun put(updateRequest: DataSpaceParticipantUpdateV7Request) {
        val requests = apiV7InboundMapper.toDataSpaceParticipantUpdateRaws(updateRequest)
        when (val result = dataSpaceParticipantUpdateApplicationService.update(requests)) {
            is DataSpaceParticipantUpdateResult.Updated -> Unit
            is DataSpaceParticipantUpdateResult.Rejected -> throw apiV7ParticipantMapper.toUpdateException(result.errors)
        }
    }
}
