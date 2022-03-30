package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PostCodeType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Postcode Response", description = "Postcode record of an address")
data class PostCodeResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full postcode denotation")
    val value: String,
    @Schema(description = "Type of specified postcode")
    val type: TypeKeyNameUrlDto<PostCodeType>
        )