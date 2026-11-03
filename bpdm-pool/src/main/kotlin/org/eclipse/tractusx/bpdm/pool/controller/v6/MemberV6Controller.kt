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
import org.eclipse.tractusx.bpdm.pool.api.v6.PoolMembersV6Api
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogEntryVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.changelog.ChangelogSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntitySearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteSearchV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v6.AddressSearchApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.ChangelogSearchApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.LegalEntitySearchApplicationV6Service
import org.eclipse.tractusx.bpdm.pool.service.application.v6.SiteSearchApplicationV6Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("MemberControllerLegacy")
class MemberV6Controller(
    private val changelogSearchApplicationService: ChangelogSearchApplicationV6Service,
    private val legalEntitySearchApplicationService: LegalEntitySearchApplicationV6Service,
    private val siteSearchApplicationService: SiteSearchApplicationV6Service,
    private val addressSearchApplicationService: AddressSearchApplicationV6Service
) : PoolMembersV6Api {


    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun searchLegalEntities(
        searchRequest: LegalEntitySearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV6Dto> {
        return legalEntitySearchApplicationService.searchMemberLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun postSiteSearch(searchRequest: SiteSearchV6Request, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseV6Dto> {
        return siteSearchApplicationService.searchMemberSites(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun searchAddresses(searchRequest: AddressSearchV6Request, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseV6Dto> {
        return addressSearchApplicationService.searchMemberAddresses(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_CHANGELOG})")
    override fun searchChangelogEntries(
        changelogSearchRequest: ChangelogSearchV6Request,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseV6Dto> {
        return changelogSearchApplicationService.searchMemberChangelogEntries(changelogSearchRequest, paginationRequest)
    }

}