package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Site", description = "Site record")
data class SiteDto(
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @Schema(description = "Site name")
    val name: String
)