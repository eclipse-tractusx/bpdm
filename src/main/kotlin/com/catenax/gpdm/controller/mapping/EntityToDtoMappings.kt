package com.catenax.gpdm.controller.mapping

import com.catenax.gpdm.controller.dto.BusinessPartnerDto
import com.catenax.gpdm.controller.dto.PageResponse
import com.catenax.gpdm.entity.BusinessPartner
import org.springframework.data.domain.Page


fun <S, T> Page<S>.toDto(dtoContent: Collection<T>) : PageResponse<T>{
    return PageResponse(this.totalElements, this.totalPages, dtoContent)
}

fun BusinessPartner.toDto() : BusinessPartnerDto{
    return BusinessPartnerDto(this.bpn)
}

