package com.catenax.gpdm.component.cdq.dto

data class LocalityCdq(
    val type: TypeKeyNameUrlCdq?,
    val shortName: String?,
    val value: String,
    val language: LanguageCdq?
)
