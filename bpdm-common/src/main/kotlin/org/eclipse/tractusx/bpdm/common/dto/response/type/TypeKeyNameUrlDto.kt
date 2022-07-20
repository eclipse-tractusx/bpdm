package org.eclipse.tractusx.bpdm.common.dto.response.type

import io.swagger.v3.oas.annotations.media.Schema

data class TypeKeyNameUrlDto <T> (
        @Schema(description = "Unique key of this type for reference")
        val technicalKey: T,
        @Schema(description = "Name or denotation of this type")
        val name: String,
        @Schema(description = "URL link leading to page with further information on the type")
        val url: String?
        )
