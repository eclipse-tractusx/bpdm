package com.catenax.gpdm.dto.request

data class IdentifierRequest (
    val value: String,
    val type: String,
    val issuingBody: String?,
    val status: String?
        )