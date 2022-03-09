package com.catenax.gpdm.component.cdq.dto

data class ThoroughfareCdq(
    var type: TypeKeyNameUrlCdq?,
    val shortName: String?,
    val number: String?,
    val value: String,
    val name: String?,
    val direction: String?,
    var language: LanguageCdq?
)
