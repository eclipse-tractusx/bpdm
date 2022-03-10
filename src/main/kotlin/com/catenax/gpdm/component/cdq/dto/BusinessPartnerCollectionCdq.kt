package com.catenax.gpdm.component.cdq.dto

data class BusinessPartnerCollectionCdq(
    val limit: Int,
    val startAfter: String?,
    val total: Int,
    val values: Collection<BusinessPartnerCdq>
)
