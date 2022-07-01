package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class AdministrativeAreaCdq(
    val value: String,
    val shortName: String? = null,
    val type: TypeKeyNameUrlCdq? = null,
    val language: LanguageCdq? = null
)
