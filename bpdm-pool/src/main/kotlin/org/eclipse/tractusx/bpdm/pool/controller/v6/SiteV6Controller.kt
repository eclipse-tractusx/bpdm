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
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolSiteV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v6.SiteCreateApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.SiteGetApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.SiteSearchApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.SiteUpdateApplicationV6Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("SiteControllerLegacy")
class SiteV6Controller(
    private val siteGetApplicationService: SiteGetApplicationV6Service,
    private val siteSearchApplicationService: SiteSearchApplicationV6Service,
    private val siteCreateApplicationService: SiteCreateApplicationV6Service,
    private val siteUpdateApplicationService: SiteUpdateApplicationV6Service
) : PoolSiteV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSite(
        bpns: String
    ): SiteWithMainAddressVerboseV6Dto {
        return siteGetApplicationService.getSite(bpns)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun postSiteSearch(
        searchRequest: SiteSearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV6Dto> {
        return siteSearchApplicationService.searchSites(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createSite(
        requests: Collection<SitePartnerCreateV6Request>
    ): SitePartnerCreateV6ResponseWrapper {
        return siteCreateApplicationService.createSitesWithMainAddress(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun updateSite(
        requests: Collection<SitePartnerUpdateV6Request>
    ): SitePartnerUpdateV6ResponseWrapper {
        return siteUpdateApplicationService.updateSites(requests)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_PARTNER})")
    override fun getSites(
        searchRequest: SiteSearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseV6Dto> {
        return postSiteSearch(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_PARTNER})")
    override fun createSiteWithLegalReference(
        request: Collection<SiteCreateWithLegalAddressAsMainV6Request>
    ): SitePartnerCreateV6ResponseWrapper {
        return siteCreateApplicationService.createSitesWithLegalAddressAsMain(request)
    }
}
