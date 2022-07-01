package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class BusinessPartnerMetadataCdq(
    val status: BusinessPartnerDocumentStatusCdq,
    val sharingStatus: SharingStatusCdq,
    val identityLinks: Collection<IdentityLinkCdq> = emptyList()
)
