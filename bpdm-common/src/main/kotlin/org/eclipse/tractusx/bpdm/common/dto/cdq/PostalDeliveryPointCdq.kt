package org.eclipse.tractusx.bpdm.common.dto.cdq

data class PostalDeliveryPointCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val number: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
