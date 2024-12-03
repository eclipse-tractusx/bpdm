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

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.PoolMemberOwnedApi.Companion.MEMBER_OWNED_PATH
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(MEMBER_OWNED_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolMemberOwnedApi {
    companion object{
        const val MEMBER_OWNED_PATH = "${ApiCommons.BASE_PATH}/member-owned"

        const val LEGAL_ENTITIES_SEARCH_PATH = "/legal-entities${CommonApiPathNames.SUBPATH_SEARCH}"
        const val SITES_SEARCH_PATH = "/sites${CommonApiPathNames.SUBPATH_SEARCH}"
        const val ADDRESSES_SEARCH_PATH = "/addresses${CommonApiPathNames.SUBPATH_SEARCH}"
    }

    @Operation(
        summary = "Search legal entities that are owned by Catena-X members",
        description = "This endpoint tries to find matches among business partners of type legal entity which are owned by Catena-X members. " +
                "Partners which entirely do not match are filtered out and the remaining partners are ranked according to the accuracy of the match. " +
                "In contrast to the `members/legal-entities` endpoint this endpoint also returns non-member legal entities that are owned by Catena-X members."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.LEGAL_ENTITIES_NAME, description = ApiCommons.LEGAL_ENTITIES_DESCRIPTION)
    @PostMapping(LEGAL_ENTITIES_SEARCH_PATH)
    fun searchLegalEntities(
        @RequestBody searchRequest: LegalEntitySearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto>

    @Operation(
        summary = "Search sites that are owned by Catena-X members",
        description = "This endpoint tries to find matches among business partners of type site which are owned by Catena-X members. " +
                "Partners which entirely do not match are filtered out and the remaining partners are ranked according to the accuracy of the match. " +
                "In contrast to the `members/sites` endpoint this endpoint also returns sites of non-member legal entities which are owned by Catena-X members."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(SITES_SEARCH_PATH)
    fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDto>

    @Operation(
        summary = "Search logistic addresses that are owned by Catena-X members",
        description = "This endpoint tries to find matches among business partners of type address which are owned by Catena-X members. " +
                "Partners which entirely do not match are filtered out and the remaining partners are ranked according to the accuracy of the match. " +
                "In contrast to the `members/addresses` endpoint this endpoint also returns addresses of non-member legal entities which are owned by Catena-X members."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.ADDRESS_NAME, description = ApiCommons.ADDRESS_DESCRIPTION)
    @PostMapping(ADDRESSES_SEARCH_PATH)
    fun searchAddresses(
        @RequestBody searchRequest: AddressSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerboseDto>

}