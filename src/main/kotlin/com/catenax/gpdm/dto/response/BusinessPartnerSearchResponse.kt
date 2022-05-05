package com.catenax.gpdm.dto.response

data class BusinessPartnerSearchResponse(
    val score: Float,
    val businessPartner: BusinessPartnerResponse
)
