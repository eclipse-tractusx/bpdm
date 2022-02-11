package com.catenax.gpdm.adapter.cdq.dto

data class BusinessPartnerMetadataCdq(
    val status: BusinessPartnerDocumentStatusCdq,
    val sharingStatus: SharingStatusCdq,
    val identityLinks: Collection<IdentityLinkCdq> = emptyList()
)
