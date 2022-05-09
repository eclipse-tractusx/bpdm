package com.catenax.gpdm.controller

import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.request.IdentifiersSearchRequest
import com.catenax.gpdm.dto.response.BpnIdentifierMappingResponse
import com.catenax.gpdm.service.BusinessPartnerFetchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/bpn")
class BpnController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val bpnConfigProperties: BpnConfigProperties
) {

    @Operation(
        summary = "Find business partner numbers by identifiers",
        description = "Find business partner numbers by identifiers. " +
                "The response can contain less results than the number of identifier values that were requested, if some of the identifiers did not exist. " +
                "For a single request, the maximum number of identifier values to search for is limited to \${bpdm.bpn.search-request-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found bpn to identifier value mappings"),
            ApiResponse(
                responseCode = "400",
                description = "On malformed request parameters or if number of requested bpns exceeds limit",
                content = [Content()]
            ),
            ApiResponse(responseCode = "404", description = "Specified identifier type not found", content = [Content()])
        ]
    )
    @PostMapping("/search")
    fun findBpnsByIdentifiers(@RequestBody identifiersSearchRequest: IdentifiersSearchRequest): ResponseEntity<Set<BpnIdentifierMappingResponse>> {
        if (identifiersSearchRequest.idValues.size > bpnConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val bpnIdentifierMappings = businessPartnerFetchService.findBpnsByIdentifiers(identifiersSearchRequest.idType, identifiersSearchRequest.idValues)
        return ResponseEntity(bpnIdentifierMappings, HttpStatus.OK)
    }
}