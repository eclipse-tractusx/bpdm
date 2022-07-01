package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class IdentityLinkCdq(
    val linkId: String,
    val cdqId: String,
    val addressId: String,
    val externalAddressId: String
)
