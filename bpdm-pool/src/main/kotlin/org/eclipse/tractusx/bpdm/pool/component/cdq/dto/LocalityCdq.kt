package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class LocalityCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
