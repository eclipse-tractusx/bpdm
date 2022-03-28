package com.catenax.gpdm.controller

import com.catenax.gpdm.dto.request.LegalFormRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.LegalFormResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.service.MetadataService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/catena")
class MetadataController (
       val metadataService: MetadataService
        ){

    @Operation(summary = "Create new identifier type",
    description = "Create a new identifier type which can be referenced by business partner records. " +
            "Identifier types such as BPN or VAT determine with which kind of values a business partner can be identified with. " +
            "The technical key can be freely chosen but needs to be unique as it is used as reference by the business partner records. " +
            "A recommendation for technical keys: They should be short, descriptive and " +
            "use a restricted common character set in order to ensure compatibility with older systems." +
            "The actual name of the identifier type is free to choose and doesn't need to be unique.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New identifier type successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "409", description = "Identifier type with specified technical key already exists", content = [Content()])
    ])
    @PostMapping("/identifier-type")
    fun createIdentifierType(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String> {
            return metadataService.createIdentifierType(type)
    }

    @Operation(summary = "Get page of identifier types",
        description = "Lists all currently known identifier types in a paginated result")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of existing identifier types, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
    ])
    @GetMapping("/identifier-type")
    fun getIdentifierTypes(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>> {
            return metadataService.getIdentifierTypes(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @Operation(summary = "Create new identifier status",
        description = "Create a new identifier status which can be referenced by business partner records. " +
                "A status further distinguishes an identifier by adding current status information such as active or revoked." +
                "The technical key can be freely chosen but needs to be unique as it is used as reference by the business partner records. " +
                "A recommendation for technical keys: They should be short, descriptive and " +
                "use a restricted common character set in order to ensure compatibility with older systems." +
                "The actual name of the identifier status is free to choose and doesn't need to be unique.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New identifier status successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "409", description = "Identifier status with specified technical key already exists", content = [Content()])
    ])
    @PostMapping("/identifier-status")
    fun createIdentifierStatus(@RequestBody status: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
        return metadataService.createIdentifierStatus(status)
    }

    @Operation(summary = "Get page of identifier statuses",
        description = "Lists all currently known identifier statuses in a paginated result")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of existing identifier statuses, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
    ])
    @GetMapping("/identifier-status")
    fun getIdentifierStati(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>> {
        return metadataService.getIdentifierStati(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @Operation(summary = "Create new issuing body",
        description = "Create a new issuing body which can be referenced by business partner records. " +
                "An issuing body should be an entity which the Catena organisation trusts to issue identifiers." +
                "The technical key can be freely chosen but needs to be unique as it is used as reference by the business partner records. " +
                "A recommendation for technical keys: They should be short, descriptive and " +
                "use a restricted common character set in order to ensure compatibility with older systems." +
                "The actual name of the issuing body is free to choose and doesn't need to be unique.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New issuing body successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "409", description = "Issuing body with specified technical key already exists", content = [Content()])
    ])
    @PostMapping("/issuing-body")
    fun createIssuingBody(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String> {
            return metadataService.createIssuingBody(type)
    }

    @Operation(summary = "Get page of issuing bodies",
        description = "Lists all currently known issuing bodies in a paginated result")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of existing issuing bodies, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
    ])
    @GetMapping("/issuing-body")
    fun getIssuingBodies(@ParameterObject paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>> {
            return metadataService.getIssuingBodies(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @Operation(summary = "Create new legal form",
        description = "Create a new legal form which can be referenced by business partner records. " +
                "The technical key can be freely chosen but needs to be unique as it is used as reference by the business partner records. " +
                "A recommendation for technical keys: They should be short, descriptive and " +
                "use a restricted common character set in order to ensure compatibility with older systems." +
                "The actual name of the legal form is free to choose and doesn't need to be unique.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "New legal form successfully created"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()]),
        ApiResponse(responseCode = "409", description = "Legal form with specified technical key already exists", content = [Content()])
    ])
    @PostMapping("/legal-form")
    fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormResponse {
            return metadataService.createLegalForm(type)
    }


    @Operation(summary = "Get page of legal forms",
        description = "Lists all currently known legal forms in a paginated result")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Page of existing legal forms, may be empty"),
        ApiResponse(responseCode = "400", description = "On malformed request parameters", content = [Content()])
    ])
    @GetMapping("/legal-form")
    fun getLegalForms(@ParameterObject paginationRequest: PaginationRequest): PageResponse<LegalFormResponse> {
            return metadataService.getLegalForms(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }
}