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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v7.PoolMembersV7Api
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressInvariantVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntitySearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteSearchV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.ChangelogSearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.LegalEntitySearchApplicationV7Service
import org.eclipse.tractusx.bpdm.pool.service.application.v7.SiteSearchApplicationV7Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberController(
    private val legalEntitySearchApplicationService: LegalEntitySearchApplicationV7Service,
    private val siteSearchApplicationService: SiteSearchApplicationV7Service,
    private val addressSearchApplicationService: AddressSearchApplicationV7Service,
    private val changelogSearchApplicationService: ChangelogSearchApplicationV7Service
) : PoolMembersV7Api {


    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun searchLegalEntities(
        searchRequest: LegalEntitySearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto> {
        return legalEntitySearchApplicationService.searchMemberLegalEntities(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun postSiteSearch(searchRequest: SiteSearchV7Request, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseV7Dto> {
        return siteSearchApplicationService.searchMemberSites(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun searchAddresses(searchRequest: AddressSearchV7Request, paginationRequest: PaginationRequest): PageDto<LogisticAddressInvariantVerboseV7Dto> {
        return addressSearchApplicationService.searchMemberAddresses(searchRequest, paginationRequest)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_CHANGELOG})")
    override fun searchChangelogEntries(
        changelogSearchRequest: ChangelogSearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseV7Dto> {
        return changelogSearchApplicationService.searchMemberChangelogEntries(changelogSearchRequest, paginationRequest)
    }

}