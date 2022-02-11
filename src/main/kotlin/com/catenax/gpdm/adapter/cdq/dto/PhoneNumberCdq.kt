package com.catenax.gpdm.adapter.cdq.dto

data class PhoneNumberCdq(
    val countryPrefix: String,
    val number: String,
    val type: TypeKeyNameUrlCdq?,
    val value: String
)
