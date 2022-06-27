package com.catenax.gpdm.dto.request

data class BusinessPartnerSearchRequest (
    val partnerProperties: BusinessPartnerPropertiesSearchRequest,
    val addressProperties: AddressPropertiesSearchRequest,
    val siteProperties: SitePropertiesSearchRequest
)