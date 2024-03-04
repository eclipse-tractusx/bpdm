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
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames
import org.eclipse.tractusx.bpdm.pool.api.PoolAddressApi.Companion.ADDRESS_PATH
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerbose
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressMatchVerboseResponse
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerUpdateResponseWrapper
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


@RequestMapping(ADDRESS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolAddressApi {

    companion object{
        const val ADDRESS_PATH = "/api/catena/addresses"
        const val PATHVAR_BPNA = "bpna"
        const val SUBPATH_BPNA = "/{$PATHVAR_BPNA}"
    }

    @Operation(
        summary = "Returns addresses by different search parameters",
        description = "This endpoint tries to find matches among all existing business partners of type address, " +
                "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
                "The match of a partner is better the higher its relevancy score. "
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
    ): PageDto<AddressMatchVerboseResponse>

    @Operation(
        summary = "Returns an address by its BPNA",
        description = "Get business partners of type address by BPNA ignoring case."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found address with specified BPNA"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No address found under specified BPNA", content = [Content()])
        ]
    )
    @GetMapping(SUBPATH_BPNA)
    fun getAddress(
        @Parameter(description = "BPNA value") @PathVariable(PATHVAR_BPNA) bpna: String
    ): LogisticAddressVerbose

    @Operation(
        summary = "Returns addresses by an array of BPNA and/or an array of corresponding BPNS and/or an array of corresponding BPNL.",
        description = "Search business partners of type address by their BPNA or their parents' BPNL or BPNS."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found sites for the specified sites and legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()])
        ]
    )
    @PostMapping(CommonApiPathNames.SUBPATH_SEARCH)
    fun searchAddresses(
        @RequestBody addressSearchRequest: AddressPartnerBpnSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<LogisticAddressVerbose>

    @Operation(
        summary = "Creates a new address",
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
    ): AddressPartnerCreateResponseWrapper

    @Operation(
        summary = "Updates an existing address",
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
    ): AddressPartnerUpdateResponseWrapper

}