package org.eclipse.tractusx.bpdm.common.dto.response.type

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Named type uniquely identified by its technical key")
data class TypeKeyNameDto <T>(
    @Schema(description = "Unique key of this type for reference")
    val technicalKey: T,
    @Schema(description = "Name or denotation of this type")
    val name: String,
)
