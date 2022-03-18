package com.catenax.gpdm.dto.response.type

import io.swagger.v3.oas.annotations.media.Schema

open class TypeKeyNameUrlDto <T> (
        @Schema(description = "Unique key of this type for reference")
        open val technicalKey: T,
        @Schema(description = "Name or denotation of this type")
        open val name: String,
        @Schema(description = "URL link leading to page with further information on the type")
        open val url: String?
        )
