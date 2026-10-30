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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.GateSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound.SharingStateDtoMapperV6
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.SharingStateSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("SharingStateControllerLegacy")
class SharingStateController(
    private val sharingStateSearchApplicationService: SharingStateSearchApplicationService,
    private val sharingStateDtoMapper: SharingStateDtoMapperV6,
    private val principalUtil: PrincipalUtil
): GateSharingStateApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_SHARING_STATE})")
    override fun getSharingStates(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<SharingStateDto> {
        val request = SharingStateSearchRaw(externalIds, sharingStateTypes = null, updatedAfter = null)
        return sharingStateSearchApplicationService.search(request, principalUtil.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(sharingStateDtoMapper::toDto)
    }
}