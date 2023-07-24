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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@RequestMapping("/api/catena", produces = [MediaType.APPLICATION_JSON_VALUE])
@HttpExchange("/api/catena")
interface GateAddressApi {


    @Operation(
        summary = "Create or update addresses.",
        description = "Create or update addresses. " +
                "Updates instead of creating a new address if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of addresses in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Addresses were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed address request", content = [Content()]),
        ]
    )
    @PutMapping("/input/addresses")
    @PutExchange("/input/addresses")
    fun upsertAddresses(@RequestBody addresses: Collection<AddressGateInputRequest>): ResponseEntity<Unit>


    @Operation(
        summary = "Get address by external identifier",
        description = "Get address by external identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found address with external identifier"),
            ApiResponse(responseCode = "404", description = "No address found under specified external identifier", content = [Content()])
        ]
    )

    @GetMapping("/input/addresses/{externalId}")
    @GetExchange("/input/addresses/{externalId}")
    fun getAddressByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): AddressGateInputDto

    @Operation(
        summary = "Get page of addresses filtered by a collection of externalIds",
        description = "Get page of addresses filtered by a collection of externalIds."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/addresses/search")
    @PostExchange("/input/addresses/search")
    fun getAddressesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<AddressGateInputDto>


    @Operation(
        summary = "Get page of addresses",
        description = "Get page of addresses."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/addresses")
    @GetExchange("/input/addresses")
    fun getAddresses(@ParameterObject @Valid paginationRequest: PaginationRequest): PageDto<AddressGateInputDto>

    @Operation(
        summary = "Get page of addresses (Output)",
        description = "Get page of addresses (Output). Can optionally be filtered by external ids."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/addresses/search")
    @PostExchange("/output/addresses/search")
    fun getAddressesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<AddressGateOutputDto>

    @Operation(
        summary = "Create or update output addresses.",
        description = "Create or update addresses (Output). " +
                "Updates instead of creating a new address if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of addresses in the request is limited to \${bpdm.api.upsert-limit} entries."

    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Addresses were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed address request", content = [Content()]),
        ]
    )
    @PutMapping("/output/addresses")
    @PutExchange("/output/addresses")
    fun putAddressesOutput(@RequestBody addresses: Collection<AddressGateOutputRequest>): ResponseEntity<Unit>


    @Operation(
        summary = "Get csv file of a page of address",
        description = "Get csv file of a page of address."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Address CSV file was successfully provided"),
            ApiResponse(responseCode = "404", description = "Address CSV file was not successful", content = [Content()])
        ]
    )

    @GetMapping("/input/addresses/search/csv")
    @GetExchange("/input/addresses/search/csv")
    fun getAddressToCsv(@ParameterObject @Valid paginationRequest: PaginationRequest): ResponseEntity<Any?>

}