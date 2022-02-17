package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import java.util.*

data class IdentifierResponse (
        val uuid: UUID,
        val value: String,
        val type: TypeKeyNameUrlDto<String>,
        val issuingBody:  TypeKeyNameUrlDto<String>?,
        val status: TypeKeyNameDto<String>?
        )