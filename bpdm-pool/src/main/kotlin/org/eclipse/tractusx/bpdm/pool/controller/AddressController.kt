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

package org.eclipse.tractusx.bpdm.pool.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.component.opensearch.SearchService
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.EntitiesWithErrorsResponse
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/addresses")
class AddressController(
    private val addressService: AddressService,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val searchService: SearchService
) {
    @Operation(
        summary = "Get page of addresses matching the search criteria",
        description = "This endpoint tries to find matches among all existing business partners of type address, " +
                "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
                "The match of a partner is better the higher its relevancy score."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of addresses matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @GetMapping
    fun getAddresses(
        @ParameterObject addressSearchRequest: AddressPartnerSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<AddressMatchResponse> {
        return searchService.searchAddresses(addressSearchRequest, paginationRequest)
    }

    @Operation(
        summary = "Get address partners by bpn",
        description = "Get business partners of type address by bpn-a ignoring case."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found address with specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No address found under specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}")
    fun getAddress(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ): AddressPartnerSearchResponse {
        return addressService.findByBpn(bpn.uppercase())
    }

    @Operation(
        summary = "Search address partners by BPNs and/or parent BPNs",
        description = "Search business partners of type address by their BPN or their parent partners BPN (BPNLs or BPNS)."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites for the specified sites and legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()])
        ]
    )
    @PostMapping("/search")
    fun searchAddresses(
        @RequestBody addressSearchRequest: AddressPartnerBpnSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<AddressPartnerSearchResponse> {
        return addressService.findByPartnerAndSiteBpns(addressSearchRequest, pageRequest)
    }

    @Operation(
        summary = "Create new address business partners",
        description = "Create new business partners of type address by specifying the BPN of the parent each address belongs to. " +
                "A parent can be either a site or legal entity business partner. " +
                "If the parent cannot be found, the record is ignored." +
                "For matching purposes, on each record you can specify your own index value which will reappear in the corresponding record of the response."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New business partner record successfully created, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @PostMapping
    fun createAddresses(
        @RequestBody
        requests: Collection<AddressPartnerCreateRequest>
    ): EntitiesWithErrorsResponse<AddressPartnerCreateResponse> {
        return businessPartnerBuildService.createAddresses(requests)
    }

    @Operation(
        summary = "Update existing address business partners",
        description = "Update existing business partner records of type address referenced via BPNA. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The successfully updated records, possible errors are returned"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @PutMapping
    fun updateAddresses(
        @RequestBody
        requests: Collection<AddressPartnerUpdateRequest>
    ): EntitiesWithErrorsResponse<AddressPartnerResponse> {
        return businessPartnerBuildService.updateAddresses(requests)
    }
}