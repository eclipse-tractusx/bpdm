package com.catenax.gpdm.component.cdq.dto

data class LocalityCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
