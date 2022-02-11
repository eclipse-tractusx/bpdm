package com.catenax.gpdm.adapter.cdq.dto

data class PostalDeliveryPointCdq(
    val type: TypeKeyNameUrlCdq?,
    val shortName: String?,
    val number: String?,
    val value: String,
    val language: LanguageCdq?
)
