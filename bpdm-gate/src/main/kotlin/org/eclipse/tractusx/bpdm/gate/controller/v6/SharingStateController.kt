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
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.BpdmSharingStateReadyException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6SharingStateMapper
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateReadyResult
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateReadyRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.SharingStateReadyApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.SharingStateSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("SharingStateControllerLegacy")
class SharingStateController(
    private val sharingStateSearchApplicationService: SharingStateSearchApplicationService,
    private val sharingStateReadyApplicationService: SharingStateReadyApplicationService,
    private val apiV6SharingStateMapper: ApiV6SharingStateMapper,
    private val tenantResolver: TenantResolver
): GateSharingStateApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_SHARING_STATE})")
    override fun getSharingStates(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<SharingStateDto> {
        val request = SharingStateSearchRaw(externalIds, sharingStateTypes = null, updatedAfter = null)
        return sharingStateSearchApplicationService.search(request, tenantResolver.resolveTenantBpnl().value, paginationRequest.toPageRequest())
            .toPageDto(apiV6SharingStateMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_SHARING_STATE})")
    override fun postSharingStateReady(request: PostSharingStateReadyRequest) {
        val readyRequest = SharingStateReadyRaw(request.externalIds)
        when (val result = sharingStateReadyApplicationService.setReady(readyRequest, tenantResolver.currentUserBpn())) {
            is SharingStateReadyResult.Readied -> Unit
            is SharingStateReadyResult.Rejected -> throw BpdmSharingStateReadyException(result.errors.map(apiV6SharingStateMapper::toMessage))
        }
    }
}
