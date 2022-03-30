package com.catenax.gpdm.controller

import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.service.BusinessPartnerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena/business-partner")
class BusinessPartnerController(
    val businessPartnerService: BusinessPartnerService,
    val bpnConfigProperties: BpnConfigProperties
) {

    @Operation(summary = "Get page of business partners matching the search criteria",
    description = "This endpoint tries to find matches among all existing business partners, " +
            "filtering out partners which entirely do not match and ranking the remaining partners according to the accuracy of the match. " +
            "The match of a partner is better the higher its relevancy score.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of business partners matching the search criteria, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed search or pagination request", content = [Content()])
    ])
    @GetMapping
    fun getBusinessPartners(
        @ParameterObject
        searchRequest: BusinessPartnerSearchRequest,
        @ParameterObject
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerResponse> {
        return businessPartnerService.findPartners(searchRequest, PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @Operation(summary = "Get business partner by identifier",
    description = "This endpoint tries to find a business partner by the specified identifier. " +
            "The identifier value is case insensitively compared but needs to be given exactly. " +
            "By default the value given is interpreted as a BPN. " +
            "By specifying the technical key of another identifier type" +
            "the value is matched against the identifiers of that given type.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found business partner with specified identifier"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "404", description = "No business partner found under specified identifier or specified identifier type not found", content = [Content()])
    ])
    @GetMapping("/{idValue}")
    fun getBusinessPartner(
        @Parameter(description = "Identifier value")
        @PathVariable
        idValue: String,
        @Parameter(description = "Type of identifier to use, defaults to BPN when omitted", schema = Schema(defaultValue = "BPN"))
        @RequestParam
        idType: String?
    ): BusinessPartnerResponse {
        val actualType = idType ?: bpnConfigProperties.id
        return if(actualType == bpnConfigProperties.id) businessPartnerService.findPartner(idValue)
        else businessPartnerService.findPartnerByIdentifier(actualType, idValue)
    }

    @Operation(summary = "Create new business partner record",
    description = "Endpoint to create new business partner records directly in the system. Currently for test purposes only.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New business partner record successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Metadata referenced by technical key not found", content = [Content()])
    ])
    @PostMapping
    fun createBusinessPartners(
        @RequestBody
        businessPartners: Collection<BusinessPartnerRequest>) : Collection<BusinessPartnerResponse>{
        return businessPartnerService.createPartners(businessPartners)
    }

}