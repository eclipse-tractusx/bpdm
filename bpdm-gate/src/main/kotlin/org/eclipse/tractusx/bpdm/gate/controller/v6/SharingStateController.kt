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
import org.eclipse.tractusx.bpdm.gate.api.v6.GateSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.SharingStateService
import org.eclipse.tractusx.bpdm.gate.util.PrincipalUtil
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto as SharingStateDtoV7

@RestController("SharingStateControllerLegacy")
class SharingStateController(
    private val sharingStateService: SharingStateService,
    private val principalUtil: PrincipalUtil
): GateSharingStateApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_SHARING_STATE})")
    override fun getSharingStates(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<SharingStateDto> {
        return sharingStateService.findSharingStates(paginationRequest, externalIds, null, null, principalUtil.resolveTenantBpnl().value)
            .let { PageDto(it.totalElements, it.totalPages, it.page, it.contentSize, content = it.content.map(::toV6Dto)) }
    }

    private fun toV6Dto(sharingStateV7: SharingStateDtoV7): SharingStateDto{
        return with(sharingStateV7){
            SharingStateDto(
                externalId = externalId,
                sharingStateType = sharingStateType,
                sharingErrorCode = sharingErrorCode,
                sharingErrorMessage = sharingErrorMessage,
                sharingProcessStarted = sharingProcessStarted,
                taskId = taskId
            )
        }
    }

}