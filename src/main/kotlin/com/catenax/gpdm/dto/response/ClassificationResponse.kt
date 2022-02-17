package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import java.util.*

data class ClassificationResponse (
        val uuid: UUID,
        val value: String,
        val code: String?,
        val type: TypeNameUrlDto?
        )