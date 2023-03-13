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

package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.service.AddressService
import org.eclipse.tractusx.bpdm.gate.service.ValidationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena")
class AddressController(
    private val addressService: AddressService,
    private val apiConfigProperties: ApiConfigProperties,
    private val validationService: ValidationService
) {
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
    fun upsertAddresses(@RequestBody addresses: Collection<AddressGateInputRequest>): ResponseEntity<Unit> {
        if (addresses.size > apiConfigProperties.upsertLimit || addresses.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        if (addresses.any {
                (it.siteExternalId == null && it.legalEntityExternalId == null) || (it.siteExternalId != null && it.legalEntityExternalId != null)
            }) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        addressService.upsertAddresses(addresses)
        return ResponseEntity(HttpStatus.OK)
    }

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
    fun getAddressByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): AddressGateInputResponse {
        return addressService.getAddressByExternalId(externalId)
    }

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
    fun getAddresses(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<AddressGateInputResponse> {
        return addressService.getAddresses(paginationRequest.limit, paginationRequest.startAfter)
    }

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
    fun getAddressesOutput(
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @RequestBody(required = false) externalIds: Collection<String>?
    ): PageStartAfterResponse<AddressGateOutput> {
        return addressService.getAddressesOutput(externalIds, paginationRequest.limit, paginationRequest.startAfter)
    }

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
    fun validateSite(
        @RequestBody addressInput: AddressGateInputRequest
    ): ValidationResponse {
        return validationService.validate(addressInput)
    }
}