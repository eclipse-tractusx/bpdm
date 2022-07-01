package org.eclipse.tractusx.bpdm.pool.dto.response.type

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Named Type With Link", description = "General type with name and URL link for further information")
data class TypeNameUrlDto (
    @Schema(description = "Name of the type")
    val name: String,
    @Schema(description = "URL link leading to page with further information on the type")
    val url: String? = null
        )