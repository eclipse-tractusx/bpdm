/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.PoolSiteApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteCreateRequestWithLegalAddressAsMain
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class SiteController(
    private val siteService: SiteService,
    private val businessPartnerBuildService: BusinessPartnerBuildService
) : PoolSiteApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSite(
        bpns: String
    ): SiteWithMainAddressVerboseDto {
        return siteService.findByBpn(bpns.uppercase())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postSiteSearch(
        searchRequest: SiteSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return siteService.searchSites(
            SiteService.SiteSearchRequest(
                siteBpns = searchRequest.siteBpns,
                legalEntityBpns = searchRequest.legalEntityBpns,
                name = searchRequest.name,
                isCatenaXMemberData = null
            ),
            paginationRequest
        )
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createSite(
        requests: Collection<SitePartnerCreateRequest>
    ): SitePartnerCreateResponseWrapper {
        return businessPartnerBuildService.createSitesWithMainAddress(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateSite(
        requests: Collection<SitePartnerUpdateRequest>
    ): SitePartnerUpdateResponseWrapper {
        return businessPartnerBuildService.updateSites(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        searchRequest: SiteSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return postSiteSearch(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createSiteWithLegalReference(
        request: Collection<SiteCreateRequestWithLegalAddressAsMain>
    ): SitePartnerCreateResponseWrapper {
        return businessPartnerBuildService.createSitesWithLegalAddressAsMain(request)
    }
}
