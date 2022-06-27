package com.catenax.gpdm.component.cdq.dto

data class BusinessPartnerDocumentStatusCdq(
    val status: BusinessPartnerDocumentStatusType,
    val explanations: Collection<String> = emptyList()
)

enum class BusinessPartnerDocumentStatusType(){
    ACCEPTED,
    REJECTED
}
