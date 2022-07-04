package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse

data class ImportResponsePage(
    val totalElements: Int,
    val nextStartAfter: String?,
    val partners: Collection<BusinessPartnerResponse>
)
