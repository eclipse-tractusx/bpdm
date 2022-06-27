package com.catenax.gpdm.component.cdq.dto

data class PremiseCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val number: String? = null,
    val value: String,
    val language: LanguageCdq? = null
)
