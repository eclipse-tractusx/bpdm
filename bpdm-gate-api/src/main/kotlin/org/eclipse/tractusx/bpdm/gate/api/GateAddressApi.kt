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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@RequestMapping("/api/catena")
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
    fun upsertAddresses (@RequestBody addresses: Collection<AddressGateInputRequest>): ResponseEntity<Unit>


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
    fun getAddressByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): AddressGateInputResponse


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
    fun getAddresses(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<AddressGateInputResponse>

    @Operation(
        summary = "Get page of addresses",
        description = "Get page of addresses. Can optionally be filtered by external ids."
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
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @RequestBody (required = false) externalIds: Collection<String>?
    ): PageOutputResponse<AddressGateOutput>

    @Operation(
        summary = "Validate an address partner",
        description = "Determines errors in an address partner record which keep it from entering the sharing process"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "A validation response with possible errors"),
            ApiResponse(responseCode = "400", description = "On malformed address requests", content = [Content()]),
        ]
    )
    @PostMapping("/input/addresses/validation")
    @PostExchange("/input/addresses/validation")
    fun validateSite(
        @RequestBody addressInput: AddressGateInputRequest
    ): ValidationResponse

}