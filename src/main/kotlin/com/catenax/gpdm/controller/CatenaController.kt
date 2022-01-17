package com.catenax.gpdm.controller

import com.catenax.gpdm.controller.dto.BusinessPartnerDto
import com.catenax.gpdm.controller.dto.PageResponse
import com.catenax.gpdm.controller.dto.PaginationRequest
import com.catenax.gpdm.controller.mapping.toDto
import com.catenax.gpdm.entity.BusinessPartner
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("catena")
class CatenaController(
    val businessPartnerRepository: BusinessPartnerRepository
) {

    @GetMapping("/businesspartners")
    fun getBusinessPartners(@Valid paginationRequest: PaginationRequest): PageResponse<BusinessPartnerDto> {
        val page = businessPartnerRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))
        return page.toDto( page.content.map { it.toDto() } )
    }

    @GetMapping("/businesspartners/{bpn}")
    fun getBusinessPartner(@RequestParam("bpn") bpn: String): BusinessPartnerDto {
        val bp =  businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

}