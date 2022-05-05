package com.catenax.gpdm.component.cdq.dto

data class PhoneNumberCdq(
    val countryPrefix: String,
    val number: String,
    val type: TypeKeyNameUrlCdq?,
    val value: String
)
