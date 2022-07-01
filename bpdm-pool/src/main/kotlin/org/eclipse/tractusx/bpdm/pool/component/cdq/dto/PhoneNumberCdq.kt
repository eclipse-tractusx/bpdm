package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class PhoneNumberCdq(
    val countryPrefix: String,
    val number: String,
    val type: TypeKeyNameUrlCdq?,
    val value: String
)
