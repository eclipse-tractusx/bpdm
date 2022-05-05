package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Classification Response", description = "Classification record of a business partner")
data class ClassificationResponse (
        val uuid: UUID,
        val value: String,
        val code: String? = null,
        val type: TypeNameUrlDto? = null
        )