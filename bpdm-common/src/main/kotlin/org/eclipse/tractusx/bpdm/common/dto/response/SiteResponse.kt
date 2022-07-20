package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Site Response", description = "Site of a legal entity")
data class SiteResponse(
    @Schema(description = "Business Partner Number, main identifier value for sites")
    val bpn: String,
    @Schema(description = "Site name")
    val name: String
)