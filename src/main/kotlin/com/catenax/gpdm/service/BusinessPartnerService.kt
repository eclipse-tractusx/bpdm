package com.catenax.gpdm.service

import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService (
    val requestConversionService: RequestConversionService,
    val businessPartnerRepository: BusinessPartnerRepository
        ){

    @Transactional
    fun findPartner(bpn: String): BusinessPartnerResponse {
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

    @Transactional
    fun findPartners(pageRequest: PageRequest): PageResponse<BusinessPartnerResponse> {
        val page = businessPartnerRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    @Transactional
    fun createPartners(bpDtos: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse>{
        val bpEntities = requestConversionService.buildBusinessPartners(bpDtos)
        businessPartnerRepository.saveAll(bpEntities)
        return bpEntities.map { it.toDto() }
    }


}