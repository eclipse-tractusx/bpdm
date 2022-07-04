package org.eclipse.tractusx.bpdm.common.dto.cdq

data class BusinessPartnerDocumentStatusCdq(
    val status: BusinessPartnerDocumentStatusType,
    val explanations: Collection<String> = emptyList()
)

enum class BusinessPartnerDocumentStatusType {
    ACCEPTED,
    REJECTED
}
