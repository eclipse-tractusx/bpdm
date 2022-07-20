package org.eclipse.tractusx.bpdm.gate.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityWithReferencesDto
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityWithReferencesResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/catena")
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
    @PutMapping("/input/legal-entities")
    fun upsertLegalEntities(@RequestBody legalEntities: Collection<LegalEntityWithReferencesDto>): ResponseEntity<Any> {
        if (legalEntities.size > apiConfigProperties.upsertLimit || containsDuplicates(legalEntities.map { it.externalId })) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        legalEntityService.upsertLegalEntities(legalEntities)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "Get legal entity by external identifier",
        description = "Get legal entity by external identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found legal entity with external identifier"),
            ApiResponse(responseCode = "404", description = "No legal entity found under specified external identifier", content = [Content()])
        ]
    )
    @GetMapping("/input/legal-entities/{externalId}")
    fun getLegalEntityByExternalId(@Parameter(description = "External identifier") @PathVariable externalId: String): LegalEntityWithReferencesDto {
        return legalEntityService.getLegalEntityByExternalId(externalId)
    }

    @Operation(
        summary = "Get page of legal entities",
        description = "Get page of legal entities."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/input/legal-entities")
    fun getLegalEntities(@ParameterObject @Valid paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<LegalEntityWithReferencesDto> {
        return legalEntityService.getLegalEntities(paginationRequest.limit, paginationRequest.startAfter)
    }

    @Operation(
        summary = "Get page of legal entities",
        description = "Get page of legal entities."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "The requested page of legal entities"),
            ApiResponse(responseCode = "400", description = "On malformed pagination request", content = [Content()]),
        ]
    )
    @GetMapping("/output/legal-entities")
    fun getLegalEntitiesOutput(
        @ParameterObject @Valid paginationRequest: PaginationStartAfterRequest,
        @Parameter(description = "Only show legal entities that were updated after the specified ISO-8601 timestamp") from: Instant?
    ): PageStartAfterResponse<LegalEntityWithReferencesResponse> {
        TODO()
    }

    @Operation(
        summary = "Get legal entity by external identifier",
        description = "Get legal entity by external identifier."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Found legal entity with external identifier"),
            ApiResponse(responseCode = "404", description = "No legal entity found under specified external identifier", content = [Content()])
        ]
    )
    @GetMapping("/output/legal-entities/{externalId}")
    fun getLegalEntityByExternalIdOutput(@Parameter(description = "External identifier") @PathVariable externalId: String): LegalEntityWithReferencesResponse {
        TODO()
    }

    private fun containsDuplicates(list: List<String>): Boolean = list.size != list.distinct().size
}