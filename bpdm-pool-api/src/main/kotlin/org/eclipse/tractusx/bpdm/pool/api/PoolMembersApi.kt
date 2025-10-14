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

package org.eclipse.tractusx.bpdm.pool.api

import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
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


@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolMembersApi {

    @Tag(name = ApiCommons.LEGAL_ENTITIES_NAME, description = ApiCommons.LEGAL_ENTITIES_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_LEGAL_ENTITIES_SEARCH_PATH_V7])
    fun searchLegalEntities(
        @RequestBody searchRequest: LegalEntitySearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto>

    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_SITES_SEARCH_PATH_V7])
    fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto>

    @Tag(name = ApiCommons.ADDRESS_NAME, description = ApiCommons.ADDRESS_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_ADDRESSES_SEARCH_PATH_V7])
    fun searchAddresses(
        @RequestBody searchRequest: AddressSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto>

    @Tag(name = ApiCommons.CHANGELOG_NAME, description = ApiCommons.CHANGELOG_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_CHANGELOG_SEARCH_PATH_V7])
    fun searchChangelogEntries(
        @RequestBody changelogSearchRequest: ChangelogSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto>
}