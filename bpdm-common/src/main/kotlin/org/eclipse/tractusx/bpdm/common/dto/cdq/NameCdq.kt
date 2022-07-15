package org.eclipse.tractusx.bpdm.common.dto.cdq

data class NameCdq(
    val value: String,
    val shortName: String? = null,
    val type: TypeKeyNameUrlCdq? = null,
    val language: LanguageCdq? = null
)
