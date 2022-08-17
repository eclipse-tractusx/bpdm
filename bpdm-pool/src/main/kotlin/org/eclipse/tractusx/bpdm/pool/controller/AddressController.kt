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
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressUpdateRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPoolResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/addresses")
class AddressController(
    private val addressService: AddressService,
    private val businessPartnerBuildService: BusinessPartnerBuildService
) {

    @Operation(
        summary = "Get address by bpn",
        description = "Get address by bpn-a of the address."
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
    ): AddressWithReferenceResponse {
        return addressService.findByBpn(bpn)
    }

    @Operation(
        summary = "Search addresses by site and/or legal entity BPNs",
        description = "Search addresses by BPNLs and BPNSs."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites for the specified sites and legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()])
        ]
    )
    @PostMapping("/search")
    fun searchAddresses(
        @RequestBody addressSearchRequest: AddressSearchRequest,
        @ParameterObject pageRequest: PaginationRequest
    ): PageResponse<AddressWithReferenceResponse> {
        return addressService.findByPartnerAndSiteBpns(addressSearchRequest, pageRequest)
    }

    @PostMapping
    fun createAddresses(
        @RequestBody
        requests: Collection<AddressRequest>
    ): Collection<AddressCreateResponse> {
        return businessPartnerBuildService.createAddresses(requests)
    }

    @PutMapping
    fun updateAddresses(
        @RequestBody
        requests: Collection<AddressUpdateRequest>
    ): Collection<AddressPoolResponse> {
        return businessPartnerBuildService.updateAddresses(requests)
    }
}