package org.eclipse.tractusx.bpdm.pool.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressWithReferenceResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.service.AddressService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/addresses")
class AddressController(
    val addressService: AddressService
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
}