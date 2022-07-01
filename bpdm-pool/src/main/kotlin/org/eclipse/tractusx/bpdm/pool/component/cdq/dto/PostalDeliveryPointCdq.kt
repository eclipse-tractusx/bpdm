package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class PostalDeliveryPointCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val number: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
