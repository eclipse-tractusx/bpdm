/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.SearchService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class SiteController(
    private val siteService: SiteService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val addressService: AddressService,
    val searchService: SearchService,
) : PoolSiteApi {

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun searchMainAddresses(
        bpnS: Collection<String>
    ): Collection<MainAddressVerboseDto> {
        return addressService.findMainAddresses(bpnS)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun getSite(
        bpns: String
    ): SiteWithMainAddressVerboseDto {
        return siteService.findByBpn(bpns.uppercase())
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun searchSites(
        siteSearchRequest: SiteBpnSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto> {
        return siteService.findByPartnerBpns(siteSearchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getChangePoolPartnerDataAsRole())")
    override fun createSite(
        requests: Collection<SitePartnerCreateRequest>
    ): SitePartnerCreateResponseWrapper {
        return businessPartnerBuildService.createSites(requests)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getChangePoolPartnerDataAsRole())")
    override fun updateSite(
        requests: Collection<SitePartnerUpdateRequest>
    ): SitePartnerUpdateResponseWrapper {
        return businessPartnerBuildService.updateSites(requests)
    }

    @PreAuthorize("hasAuthority(@poolSecurityConfigProperties.getReadPoolPartnerDataAsRole())")
    override fun getSitesPaginated(
        paginationRequest: PaginationRequest
    ): PageDto<SiteMatchVerboseDto> {
        return searchService.searchSites(paginationRequest)
    }
}
