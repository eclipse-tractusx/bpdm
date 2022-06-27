package com.catenax.gpdm.dto

import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.entity.BusinessPartner

data class BusinessPartnerUpdateDto (
    val businessPartner: BusinessPartner,
    val updateRequest: BusinessPartnerRequest
)