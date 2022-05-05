package com.catenax.gpdm.component.cdq.dto

data class AdministrativeAreaCdq(
    val value: String,
    val shortName: String? = null,
    val type: TypeKeyNameUrlCdq? = null,
    val language: LanguageCdq? = null
)
