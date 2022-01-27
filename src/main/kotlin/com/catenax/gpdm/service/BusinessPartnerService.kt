package com.catenax.gpdm.service

import com.catenax.gpdm.controller.dto.BusinessPartnerBaseDto
import com.catenax.gpdm.controller.dto.BusinessPartnerDto
import com.catenax.gpdm.controller.dto.PageResponse
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService (
    val dtoToEntityService: DtoToEntityService,
    val businessPartnerRepository: BusinessPartnerRepository
        ){

    @Transactional
    fun findPartner(bpn: String): BusinessPartnerDto{
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

    @Transactional
    fun findPartners(pageRequest: PageRequest): PageResponse<BusinessPartnerDto> {
        val page = businessPartnerRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    @Transactional
    fun createPartners(bpDtos: Collection<BusinessPartnerBaseDto>): Collection<BusinessPartnerDto>{
        val bpEntities = dtoToEntityService.buildBusinessPartners(bpDtos)
        businessPartnerRepository.saveAll(bpEntities)
        return bpEntities.map { it.toDto() }
    }


}