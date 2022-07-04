package org.eclipse.tractusx.bpdm.common.dto.cdq

data class BusinessPartnerMetadataCdq(
    val status: BusinessPartnerDocumentStatusCdq?,
    val sharingStatus: SharingStatusCdq?,
    val identityLinks: Collection<IdentityLinkCdq> = emptyList()
)
