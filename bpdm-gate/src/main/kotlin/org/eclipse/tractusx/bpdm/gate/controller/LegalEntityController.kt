package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/legal-entities")
class LegalEntityController(
    val legalEntityService: LegalEntityService,
    val apiConfigProperties: ApiConfigProperties
) {

    @Operation(
        summary = "Create or update legal entities.",
        description = "Create or update legal entities. " +
                "Updates instead of creating a new legal entity if an already existing external id is used. " +
                "The same external id may not occur more than once in a single request. " +
                "For a single request, the maximum number of legal entities in the request is limited to \${bpdm.api.upsert-limit} entries."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Legal entities were successfully updated or created"),
            ApiResponse(responseCode = "400", description = "On malformed legal entity request", content = [Content()]),
        ]
    )
    @PutMapping
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityRequest>): ResponseEntity<Any> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || containsDuplicates(legalEntities.map { it.externalId })) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntities(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

    private fun containsDuplicates(list: List<String>): Boolean = list.size != list.distinct().size
}