package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.PostCodeType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Postcode Request", description = "New postcode record for an address")
data class PostCodeRequest (
    @Schema(description = "Full postcode denotation")
    val value: String,
    @Schema(description = "Type of specified postcode", defaultValue = "OTHER")
    val type: PostCodeType = PostCodeType.OTHER
        )