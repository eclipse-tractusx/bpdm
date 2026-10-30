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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.GateRelationSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7RelationMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.RelationSharingStateSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class RelationSharingStateController(
    private val principalUtil: PrincipalUtil,
    private val searchApplicationService: RelationSharingStateSearchApplicationService,
    private val apiV7RelationMapper: ApiV7RelationMapper,
    private val apiV7InboundMapper: ApiV7InboundMapper
): GateRelationSharingStateApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_SHARING_STATE})")
    override fun get(
        externalIds: Collection<String>?,
        sharingStateTypes: Collection<RelationSharingStateType>?,
        updatedAfter: Instant?,
        paginationRequest: PaginationRequest
    ): PageDto<RelationSharingStateDto> {
        val request = RelationSharingStateSearchRaw(externalIds, sharingStateTypes?.map(apiV7InboundMapper::toModel), updatedAfter)
        return searchApplicationService.search(request, principalUtil.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(apiV7RelationMapper::toDto)
    }
}