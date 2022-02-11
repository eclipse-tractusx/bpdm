package com.catenax.gpdm.adapter.cdq.dto

data class IdentifierCdq(
    val type: TypeKeyNameUrlCdq?,
    val value: String,
    val issuingBody:  TypeKeyNameUrlCdq?,
    val status: TypeKeyNameCdq?
)
