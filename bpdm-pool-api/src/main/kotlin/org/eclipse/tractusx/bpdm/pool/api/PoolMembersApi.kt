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

package org.eclipse.tractusx.bpdm.pool.api

import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.PoolMembersApi.Companion.MEMBERS_PATH
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping


@RequestMapping(MEMBERS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolMembersApi {

    companion object{
        const val MEMBERS_PATH = "members"

        const val LEGAL_ENTITIES_SEARCH_PATH = "/legal-entities${CommonApiPathNames.SUBPATH_SEARCH}"
        const val SITES_SEARCH_PATH = "/sites${CommonApiPathNames.SUBPATH_SEARCH}"
        const val ADDRESSES_SEARCH_PATH = "/addresses${CommonApiPathNames.SUBPATH_SEARCH}"
        const val CHANGELOG_SEARCH_PATH = "/changelog/search"
    }

    @Tag(name = ApiTags.LEGAL_ENTITIES_NAME, description = ApiTags.LEGAL_ENTITIES_DESCRIPTION)
    @PostMapping(LEGAL_ENTITIES_SEARCH_PATH)
    fun searchLegalEntities(
        @RequestBody searchRequest: LegalEntitySearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto>

    @Tag(name = ApiTags.SITE_NAME, description = ApiTags.SITE_DESCRIPTION)
    @PostMapping(SITES_SEARCH_PATH)
    fun searchSites(
        @RequestBody searchRequest: SiteSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto>

    @Tag(name = ApiTags.ADDRESS_NAME, description = ApiTags.ADDRESS_DESCRIPTION)
    @PostMapping(ADDRESSES_SEARCH_PATH)
    fun searchAddresses(
        @RequestBody searchRequest: AddressSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto>

    @Tag(name = ApiTags.CHANGELOG_NAME, description = ApiTags.CHANGELOG_DESCRIPTION)
    @PostMapping(CHANGELOG_SEARCH_PATH)
    fun searchChangelogEntries(
        @RequestBody changelogSearchRequest: ChangelogSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto>
}