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

package org.eclipse.tractusx.bpdm.pool.api.v6

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.AddressSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.ChangelogSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntitySearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ChangelogEntryVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping


@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolMembersV6Api {

    @Operation(deprecated = true)
    @Tag(name = ApiCommons.LEGAL_ENTITIES_NAME, description = ApiCommons.LEGAL_ENTITIES_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_LEGAL_ENTITIES_SEARCH_PATH_V6])
    fun searchLegalEntities(
        @RequestBody searchRequest: LegalEntitySearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDtoV6>

    @Operation(deprecated = true)
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_SITES_SEARCH_PATH_V6])
    fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDtoV6>

    @Operation(deprecated = true)
    @Tag(name = ApiCommons.ADDRESS_NAME, description = ApiCommons.ADDRESS_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_ADDRESSES_SEARCH_PATH_V6])
    fun searchAddresses(
        @RequestBody searchRequest: AddressSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDtoV6>

    @Operation(deprecated = true)
    @Tag(name = ApiCommons.CHANGELOG_NAME, description = ApiCommons.CHANGELOG_DESCRIPTION)
    @PostMapping(value = [ApiCommons.MEMBERS_CHANGELOG_SEARCH_PATH_V6])
    fun searchChangelogEntries(
        @RequestBody changelogSearchRequest: ChangelogSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDtoV6>
}