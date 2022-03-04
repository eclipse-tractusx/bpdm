package com.catenax.gpdm.controller

import com.catenax.gpdm.dto.request.LegalFormRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.LegalFormResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.service.MetadataService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/catena")
class MetadataController (
       val metadataService: MetadataService
        ){
        @PostMapping("/identifier-type")
        fun createIdentifierType(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String> {
                return metadataService.createIdentifierType(type)
        }

        @GetMapping("/identifier-type")
        fun getIdentifierTypes(@Valid paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>> {
                return metadataService.getIdentifierTypes(PageRequest.of(paginationRequest.page, paginationRequest.size))
        }

        @PostMapping("/identifier-status")
        fun createIdentifierStatus(@RequestBody status: TypeKeyNameDto<String>): TypeKeyNameDto<String> {
            return metadataService.createIdentifierStatus(status)
        }
    
        @GetMapping("/identifier-status")
        fun getIdentifierStati(@Valid paginationRequest: PaginationRequest): PageResponse<TypeKeyNameDto<String>> {
            return metadataService.getIdentifierStati(PageRequest.of(paginationRequest.page, paginationRequest.size))
        }

        @PostMapping("/issuing-body")
        fun createIssuingBody(@RequestBody type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String> {
                return metadataService.createIssuingBody(type)
        }

        @GetMapping("/issuing-body")
        fun getIssuingBodies(@Valid paginationRequest: PaginationRequest): PageResponse<TypeKeyNameUrlDto<String>> {
                return metadataService.getIssuingBodies(PageRequest.of(paginationRequest.page, paginationRequest.size))
        }

        @PostMapping("/legal-form")
        fun createLegalForm(@RequestBody type: LegalFormRequest): LegalFormResponse {
                return metadataService.createLegalForm(type)
        }


        @GetMapping("/legal-form")
        @Operation(method = "getLegalForms")
        fun getLegalForms(@Valid paginationRequest: PaginationRequest): PageResponse<LegalFormResponse> {
                return metadataService.getLegalForms(PageRequest.of(paginationRequest.page, paginationRequest.size))
        }
}