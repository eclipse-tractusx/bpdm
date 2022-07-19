package org.eclipse.tractusx.bpdm.common.dto.cdq

data class LocalityCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
