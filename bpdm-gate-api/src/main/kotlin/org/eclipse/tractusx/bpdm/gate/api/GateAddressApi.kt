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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface GateAddressApi {

    @Operation(
        summary = "Creates or updates an existing address in the input stage",
        description = "Create or update addresses. " +
                "Updates instead of creating a new address if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of addresses in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Addresses were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed address request", content = [Content()]),
        ]
    )
    @PutMapping("/input/addresses")
    fun upsertAddresses(@RequestBody addresses: Collection<AddressGateInputRequest>): ResponseEntity<Unit>

    @Operation(
        summary = "Returns address by external ID from the input stage",
        description = "Returns address by external ID from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found address with external ID"),
            ApiResponse(responseCode = "404", description = "No address found under specified external ID", content = [Content()])
        ]
    )
    @GetMapping("/input/addresses/{externalId}")
    fun getAddressByExternalId(@Parameter(description = "External ID") @PathVariable externalId: String): AddressGateInputDto

    @Operation(
        summary = "Returns addresses by an array of external IDs from the input stage",
        description = "Returns page of addresses from the input stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/input/addresses/search")
    fun getAddressesByExternalIds(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody externalIds: Collection<String>
    ): PageDto<AddressGateInputDto>

    @Operation(
        summary = "Returns addresses from the input stage",
        description = "Returns page of addresses from the input stage.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/addresses")
    fun getAddresses(@ParameterObject @Valid paginationRequest: PaginationRequest): PageDto<AddressGateInputDto>

    @Operation(
        summary = "Returns addresses by an array of external IDs from the output stage",
        description = "Get page of addresses from the output stage. Can optionally be filtered by external IDs.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of addresses"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @PostMapping("/output/addresses/search")
    fun getAddressesOutput(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageDto<AddressGateOutputDto>

    @Operation(
        summary = "Creates or updates an existing address in the output stage",
        description = "Create or update addresses (Output). " +
                "Updates instead of creating a new address if an already existing external ID is used. " +
                "The same external ID may not occur more than once in a single request. " +
                "For a single request, the maximum number of addresses in the request is limited to \${bpdm.api.upsert-limit} entries.",
        deprecated = true

    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Addresses were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed address request", content = [Content()]),
        ]
    )
    @PutMapping("/output/addresses")
    fun upsertAddressesOutput(@RequestBody addresses: Collection<AddressGateOutputRequest>): ResponseEntity<Unit>
}
