package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeNameUrlDto

data class ClassificationResponse (
        val value: String,
        val code: String?,
        val type: TypeNameUrlDto?
        )