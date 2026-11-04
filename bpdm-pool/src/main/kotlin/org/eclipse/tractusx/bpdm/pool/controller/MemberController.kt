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
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
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
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7ChangelogMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7LegalEntityMapper
import org.eclipse.tractusx.bpdm.pool.mapper.apiv7.ApiV7SiteMapper
import org.eclipse.tractusx.bpdm.pool.model.output.changelog.ChangelogSearchResult
import org.eclipse.tractusx.bpdm.pool.service.application.ChangelogSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.LegalEntitySearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.SiteSearchApplicationService
import org.eclipse.tractusx.bpdm.pool.service.application.v7.AddressSearchApplicationV7Service
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberController(
    private val legalEntitySearchApplicationService: LegalEntitySearchApplicationService,
    private val siteSearchApplicationService: SiteSearchApplicationService,
    private val addressSearchApplicationService: AddressSearchApplicationV7Service,
    private val changelogSearchApplicationService: ChangelogSearchApplicationService,
    private val apiV7InboundMapper: ApiV7InboundMapper,
    private val apiV7ChangelogMapper: ApiV7ChangelogMapper,
    private val apiV7LegalEntityMapper: ApiV7LegalEntityMapper,
    private val apiV7SiteMapper: ApiV7SiteMapper
):PoolMembersV7Api {


    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun searchLegalEntities(
        searchRequest: LegalEntitySearchV7Request,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseV7Dto> {
        val request = apiV7InboundMapper.toLegalEntitySearchRaw(searchRequest)
        return legalEntitySearchApplicationService.searchMembers(request, paginationRequest.toPageRequest())
            .toPageDto(apiV7LegalEntityMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_MEMBER_PARTNER})")
    override fun postSiteSearch(searchRequest: SiteSearchV7Request, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseV7Dto> {
        val request = apiV7InboundMapper.toSiteSearchRaw(searchRequest)
        return siteSearchApplicationService.searchMembers(request, paginationRequest.toPageRequest())
            .toPageDto(apiV7SiteMapper::toDto)
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
        val request = apiV7InboundMapper.toChangelogSearchRaw(changelogSearchRequest)
        return when (val result = changelogSearchApplicationService.searchMembers(request, paginationRequest.toPageRequest())) {
            is ChangelogSearchResult.Found -> result.page.toPageDto(apiV7ChangelogMapper::toDto)
            is ChangelogSearchResult.Rejected -> throw apiV7ChangelogMapper.toException(result.errors)
        }
    }

}