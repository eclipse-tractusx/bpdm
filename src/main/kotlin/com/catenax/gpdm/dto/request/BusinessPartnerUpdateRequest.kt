package com.catenax.gpdm.dto.request

data class BusinessPartnerUpdateRequest(
    val bpn: String,
    val values: BusinessPartnerRequest
)
