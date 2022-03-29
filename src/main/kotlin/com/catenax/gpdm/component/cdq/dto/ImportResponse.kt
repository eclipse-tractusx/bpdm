package com.catenax.gpdm.component.cdq.dto

import com.catenax.gpdm.dto.response.BusinessPartnerResponse

data class ImportResponse(
    val nextStartAfter: String?,
    val partners: Collection<BusinessPartnerResponse>
)
