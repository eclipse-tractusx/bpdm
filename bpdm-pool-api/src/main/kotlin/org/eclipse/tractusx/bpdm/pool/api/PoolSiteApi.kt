/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.MainAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerUpdateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePoolVerboseDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@RequestMapping("/api/catena/sites", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/catena/sites")
interface PoolSiteApi {


    @Operation(
        summary = "Search Main Addresses",
        description = "Search main addresses of site business partners by BPNS"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The found main addresses"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ]
    )
    @PostMapping("/main-addresses/search", produces = ["application/json"])
    @PostExchange("/main-addresses/search")
    fun searchMainAddresses(
        @RequestBody
        bpnS: Collection<String>
    ): Collection<MainAddressVerboseDto>

    @Operation(
        summary = "Get site partners by bpn",
        description = "Get business partners of type site by bpn-s ignoring case."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No site found under specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}")
    @GetExchange("/{bpn}")
    fun getSite(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ): SitePoolVerboseDto

    @Operation(
        summary = "Search site partners by BPNs and/or parent BPNs",
        description = "Search business partners of type site by their BPNSs or by the BPNLs of their parent legal entities"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites that belong to specified legal entites"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
        ]
    )
    @PostMapping("/search")
    @PostExchange("/search")
    fun searchSites(
        @RequestBody siteSearchRequest: SiteBpnSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<SitePoolVerboseDto>

    @Operation(
        summary = "Create new site business partners",
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
    @PostMapping
    @PostExchange
    fun createSite(
        @RequestBody
        requests: Collection<SitePartnerCreateRequest>
    ): SitePartnerCreateResponseWrapper

    @Operation(
        summary = "Update existing site business partners",
        description = "Update existing business partner records of type site referenced via BPNS. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Update sites request was processed successfully, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @PutMapping
    @PutExchange
    fun updateSite(
        @RequestBody
        requests: Collection<SitePartnerUpdateRequest>
    ): SitePartnerUpdateResponseWrapper
}