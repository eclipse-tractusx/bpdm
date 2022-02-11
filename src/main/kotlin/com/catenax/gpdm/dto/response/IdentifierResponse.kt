package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto

data class IdentifierResponse (
        val value: String,
        val type: TypeKeyNameUrlDto<String>,
        val issuingBody:  TypeKeyNameUrlDto<String>?,
        val status: TypeKeyNameDto<String>?
        )