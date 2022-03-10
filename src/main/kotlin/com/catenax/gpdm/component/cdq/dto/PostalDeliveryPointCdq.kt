package com.catenax.gpdm.component.cdq.dto

data class PostalDeliveryPointCdq(
    val type: TypeKeyNameUrlCdq?,
    val shortName: String?,
    val number: String?,
    val value: String,
    val language: LanguageCdq?
)
