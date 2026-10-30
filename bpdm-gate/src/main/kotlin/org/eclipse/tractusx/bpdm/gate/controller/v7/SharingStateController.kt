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

package org.eclipse.tractusx.bpdm.gate.controller.v7

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.GateSharingStateV7Api
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.PostSharingStateReadyV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.exception.BpdmSharingStateReadyException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7SharingStateMapper
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateReadyResult
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateReadyRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.SharingStateReadyApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.SharingStateSearchApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class SharingStateController(
    private val sharingStateSearchApplicationService: SharingStateSearchApplicationService,
    private val sharingStateReadyApplicationService: SharingStateReadyApplicationService,
    private val apiV7SharingStateMapper: ApiV7SharingStateMapper,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val tenantResolver: TenantResolver
): GateSharingStateV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_SHARING_STATE})")
    override fun getSharingStates(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?,
        sharingStateTypes: Collection<SharingStateTypeV7>?,
        updatedAfter: Instant?,
    ): PageDto<SharingStateV7Dto> {
        val request = SharingStateSearchRaw(externalIds, sharingStateTypes?.map(apiV7InboundMapper::toModel), updatedAfter)
        return sharingStateSearchApplicationService.search(request, tenantResolver.currentUserBpn(), paginationRequest.toPageRequest())
            .toPageDto(apiV7SharingStateMapper::toDto)
    }


    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_SHARING_STATE})")
    override fun postSharingStateReady(request: PostSharingStateReadyV7Request) {
        val readyRequest = SharingStateReadyRaw(request.externalIds)
        when (val result = sharingStateReadyApplicationService.setReady(readyRequest, tenantResolver.currentUserBpn())) {
            is SharingStateReadyResult.Readied -> Unit
            is SharingStateReadyResult.Rejected -> throw BpdmSharingStateReadyException(result.errors.map(apiV7SharingStateMapper::toMessage))
        }
    }
}
