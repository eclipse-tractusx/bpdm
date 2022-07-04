package org.eclipse.tractusx.bpdm.pool.dto.response

data class BusinessPartnerSearchResponse(
    val score: Float,
    val businessPartner: BusinessPartnerResponse
)
