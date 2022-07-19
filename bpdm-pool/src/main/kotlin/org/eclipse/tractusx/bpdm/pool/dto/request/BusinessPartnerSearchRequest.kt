package org.eclipse.tractusx.bpdm.pool.dto.request

data class BusinessPartnerSearchRequest (
    val partnerProperties: BusinessPartnerPropertiesSearchRequest,
    val addressProperties: AddressPropertiesSearchRequest,
    val siteProperties: SitePropertiesSearchRequest
)