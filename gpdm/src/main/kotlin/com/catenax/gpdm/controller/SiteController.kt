package com.catenax.gpdm.controller

import com.catenax.gpdm.dto.response.SiteWithReferenceResponse
import com.catenax.gpdm.service.SiteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/sites")
class SiteController(
    val siteService: SiteService
) {

    @Operation(
        summary = "Get site by bpn",
        description = "Get site by bpn-s of the site."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found site with specified bpn"),
            ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
            ApiResponse(responseCode = "404", description = "No site found under specified bpn", content = [Content()])
        ]
    )
    @GetMapping("/{bpn}")
    fun getSite(
        @Parameter(description = "Bpn value") @PathVariable bpn: String
    ): SiteWithReferenceResponse {
        return siteService.findByBpn(bpn)
    }
}