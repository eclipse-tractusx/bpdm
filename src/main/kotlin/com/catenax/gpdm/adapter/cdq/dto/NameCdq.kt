package com.catenax.gpdm.adapter.cdq.dto

data class NameCdq(
    val value: String,
    val shortName: String?,
    val type: TypeKeyNameUrlCdq?,
    val language: LanguageCdq?
)
