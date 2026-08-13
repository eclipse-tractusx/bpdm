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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.ApiCommons
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteCreateRequestWithLegalAddressAsMainV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteSearchRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolSiteV6Api {

    @Operation(
        deprecated = true,
        summary = "Returns a site by its BPNS",
        description = "Get business partners of type site by BPNS ignoring case."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with specified BPNS"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No site found under specified BPNS", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @GetMapping(value = ["${ApiCommons.SITE_BASE_PATH_V6}/{bpns}"])
    fun getSite(
        @Parameter(description = "BPNS value") @PathVariable bpns: String
    ): SiteWithMainAddressVerboseDtoV6

    @Operation(
        deprecated = true,
        summary = "Returns sites by an array of BPNS and/or an array of corresponding BPNL",
        description = "Search business partners of type site by their BPNSs or by the BPNLs of their parent legal entities"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites that belong to specified legal entites"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(value = ["${ApiCommons.SITE_BASE_PATH_V6}/search"])
    fun postSiteSearch(
        @RequestBody searchRequest: SiteSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDtoV6>

    @Operation(
        deprecated = true,
        summary = "Creates a new site",
        description = "Create new business partners of type site by specifying the BPNL of the legal entity each site belongs to. " +
                "If the legal entitiy cannot be found, the record is ignored." +
                "For matching purposes, on each record you can specify your own index value which will reappear in the corresponding record of the response."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New sites request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(value = [ApiCommons.SITE_BASE_PATH_V6])
    fun createSite(
        @RequestBody
        requests: Collection<SitePartnerCreateRequestV6>
    ): SitePartnerCreateResponseWrapperV6

    @Operation(
        deprecated = true,
        summary = "Updates an existing site",
        description = "Update existing business partner records of type site referenced via BPNS. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Update sites request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PutMapping(value = [ApiCommons.SITE_BASE_PATH_V6])
    fun updateSite(
        @RequestBody
        requests: Collection<SitePartnerUpdateRequestV6>
    ): SitePartnerUpdateResponseWrapperV6

    @Operation(
        deprecated = true,
        summary = "Get page of sites matching the pagination search criteria",
        description = "This endpoint retrieves all existing business partners of type sites."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @GetMapping(value = [ApiCommons.SITE_BASE_PATH_V6])
    fun getSites(
        @ParameterObject searchRequest: SiteSearchRequestV6,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<SiteWithMainAddressVerboseDtoV6>

    @Operation(
        deprecated = true,
        summary = "Create a new site with legal entity reference",
        description = "Create a business partner site with the given legal entity reference. " +
                "It will designate the address information as both legal and site main address."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New sites request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()])
        ]
    )
    @Tag(name = ApiCommons.SITE_NAME, description = ApiCommons.SITE_DESCRIPTION)
    @PostMapping(value = ["${ApiCommons.SITE_BASE_PATH_V6}/legal-main-sites"])
    fun createSiteWithLegalReference(
        @RequestBody request: Collection<SiteCreateRequestWithLegalAddressAsMainV6>
    ): SitePartnerCreateResponseWrapperV6
}