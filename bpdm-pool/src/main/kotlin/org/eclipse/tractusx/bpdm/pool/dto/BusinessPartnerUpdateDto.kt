package org.eclipse.tractusx.bpdm.pool.dto

import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner

data class BusinessPartnerUpdateDto (
    val businessPartner: BusinessPartner,
    val updateRequest: BusinessPartnerRequest
)