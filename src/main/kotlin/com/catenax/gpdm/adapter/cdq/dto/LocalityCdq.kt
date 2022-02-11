package com.catenax.gpdm.adapter.cdq.dto

data class LocalityCdq(
    val type: TypeKeyNameUrlCdq?,
    val shortName: String?,
    val value: String,
    val language: LanguageCdq?
)
