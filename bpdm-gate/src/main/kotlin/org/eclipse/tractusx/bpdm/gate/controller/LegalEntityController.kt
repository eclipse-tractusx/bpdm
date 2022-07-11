package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/legal-entities")
class LegalEntityController(
    val legalEntityService: LegalEntityService
) {

    @Operation(
        summary = "Create or update legal entity"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityRequest>) {
        legalEntityService.upsertLegalEntities(legalEntities)
    }
}