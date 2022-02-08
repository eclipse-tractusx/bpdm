package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.IdentifierStatus

data class IdentifierRequest (
    val value: String,
    val type: String,
    val issuingBody: String,
    val status: IdentifierStatus
        )