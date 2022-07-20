package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.AddressWithReferencesDto
import org.eclipse.tractusx.bpdm.common.dto.response.AddressWithReferencesResponse
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/catena")
class AddressController {

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
    fun upsertAddresses(@RequestBody addresses: Collection<AddressWithReferencesDto>): ResponseEntity<Any> {
        TODO()
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
    fun getAddressByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): AddressWithReferencesDto {
        TODO()
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
    fun getAddresses(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<AddressWithReferencesDto> {
        TODO()
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
    @GetMapping("/output/addresses")
    fun getAddressesOutput(
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @Parameter(description = "Only show addresses that were updated after the specified ISO-8601 timestamp") from: Instant?
    ): PageStartAfterResponse<AddressWithReferencesResponse> {
        TODO()
    }
}