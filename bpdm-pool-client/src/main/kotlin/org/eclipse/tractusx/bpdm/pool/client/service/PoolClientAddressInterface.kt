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

package org.eclipse.tractusx.bpdm.pool.client.service

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse

import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressMatchResponse
import org.eclipse.tractusx.bpdm.pool.client.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange



@RequestMapping("/api/catena/addresses")
@HttpExchange("/api/catena/addresses")
interface PoolClientAddressInterface {

    @Operation(
        summary = "Get page of addresses matching the search criteria",
        description = "This endpoint tries to find matches among all existing business partners of type address, " +
                "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
                "The match of a partner is better the higher its relevancy score. " +
                "Note that when using search parameters the max page is \${bpdm.opensearch.max-page}."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of addresses matching the search criteria, may be empty"),
            ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
        ]
    )
    @GetMapping
    @GetExchange
    fun getAddresses(
        @ParameterObject addressSearchRequest: AddressPartnerSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<AddressMatchResponse>

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
    @GetExchange("/{bpn}")
    fun getAddress(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ): AddressPartnerSearchResponse
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
    @PostExchange("/search")
    fun searchAddresses(
        @RequestBody addressSearchRequest: AddressPartnerBpnSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageResponse<AddressPartnerSearchResponse>

    @Operation(
        summary = "Create new address business partners",
        description = "Create new business partners of type address by specifying the BPN of the parent each address belongs to. " +
                "A parent can be either a site or legal entity business partner. " +
                "If the parent cannot be found, the record is ignored." +
                "For matching purposes, on each record you can specify your own index value which will reappear in the corresponding record of the response."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "New business partner record successfully created"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @PostMapping
    @PostExchange
    fun createAddresses(
        @RequestBody
        requests: Collection<AddressPartnerCreateRequest>
    ): Collection<AddressPartnerCreateResponse>

    @Operation(
        summary = "Update existing address business partners",
        description = "Update existing business partner records of type address referenced via BPNA. " +
                "The endpoint expects to receive the full updated record, including values that didn't change."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The successfully updated records"),
            ApiResponse(responseCode = "400", description = "On malformed requests", content = [Content()])
        ]
    )
    @PutMapping
    @PutExchange
    fun updateAddresses(
        @RequestBody
        requests: Collection<AddressPartnerUpdateRequest>
    ): Collection<AddressPartnerResponse>


}