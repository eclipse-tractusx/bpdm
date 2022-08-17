/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteUpsertResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SiteWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.SiteService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/sites")
class SiteController(
    private val siteService: SiteService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val addressService: AddressService
) {

    @PostMapping("/main-addresses/search")
    fun searchMainAddresses(bpnS: Collection<String>): Collection<MainAddressSearchResponse> {
        return addressService.findMainAddresses(bpnS)
    }

    @Operation(
        summary = "Get site by bpn",
        description = "Get site by bpn-s of the site."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No site found under specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}")
    fun getSite(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ): SiteWithReferenceResponse {
        return siteService.findByBpn(bpn)
    }

    @Operation(
        summary = "Search sites by BPNLs",
        description = "Search sites by legal entity BPNs"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites that belong to specified legal entites"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @PostMapping("/search")
    fun searchSites(
        @RequestBody siteSearchRequest: SiteSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<SiteWithReferenceResponse> {
        return siteService.findByPartnerBpns(siteSearchRequest, paginationRequest)
    }

    @PostMapping
    fun createSite(
        @RequestBody
        requests: Collection<SiteCreateRequest>
    ): Collection<SiteUpsertResponse> {
        return businessPartnerBuildService.createSites(requests)
    }

    @PutMapping
    fun updateSite(
        @RequestBody
        requests: Collection<SiteUpdateRequest>
    ): Collection<SiteUpsertResponse> {
        return businessPartnerBuildService.updateSites(requests)
    }
}