package com.catenax.gpdm.component.cdq.dto

data class IdentifierCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val value: String,
    val issuingBody: TypeKeyNameUrlCdq? = null,
    val status: TypeKeyNameCdq? = null
)
