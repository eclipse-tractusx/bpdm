package com.catenax.gpdm.controller

import com.catenax.gpdm.controller.dto.*
import com.catenax.gpdm.service.BusinessPartnerService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("catena")
class CatenaController(
    val businessPartnerService: BusinessPartnerService
) {

    @GetMapping("/businesspartners")
    fun getBusinessPartners(@Valid paginationRequest: PaginationRequest): PageResponse<BusinessPartnerDto> {
        return businessPartnerService.findPartners(PageRequest.of(paginationRequest.page, paginationRequest.size))
    }

    @GetMapping("/businesspartners/{bpn}")
    fun getBusinessPartner(@PathVariable bpn: String): BusinessPartnerDto {
        return businessPartnerService.findPartner(bpn)
    }

    @PostMapping("/businesspartners")
    fun createBusinessPartners(@RequestBody businessPartners: Collection<BusinessPartnerBaseDto>) : Collection<BusinessPartnerDto>{
        return businessPartnerService.createPartners(businessPartners)
    }

}