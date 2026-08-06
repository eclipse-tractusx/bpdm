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
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolCxMembershipV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.CxMembershipDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.CxMembershipUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v6.CxMembershipSearchApplicationV6Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("CxMembershipControllerLegacy")
class CxMembershipV6Controller(
    private val cxMembershipSearchApplicationV6Service: CxMembershipSearchApplicationV6Service,
    private val cxMembershipLegacyServiceMapper: CxMembershipLegacyServiceMapper
): PoolCxMembershipV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun get(searchRequest: CxMembershipSearchRequestV6, paginationRequest: PaginationRequest): PageDto<CxMembershipDtoV6> {
       return cxMembershipSearchApplicationV6Service.searchMemberships(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun put(updateRequest: CxMembershipUpdateRequestV6) {
        return cxMembershipLegacyServiceMapper.updateMemberships(updateRequest)
    }
}