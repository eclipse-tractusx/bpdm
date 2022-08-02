package org.eclipse.tractusx.bpdm.common.dto.cdq

data class PhoneNumberCdq(
    val countryPrefix: String?,
    val number: String?,
    val type: TypeKeyNameUrlCdq?,
    val value: String
)
