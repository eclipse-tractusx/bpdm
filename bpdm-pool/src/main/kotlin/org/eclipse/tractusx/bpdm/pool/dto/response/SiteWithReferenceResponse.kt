package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Site With Reference Response", description = "Site with bpn reference")
data class SiteWithReferenceResponse(
    val site: SiteResponse,
    @Schema(description = "Business Partner Number of the related legal entity")
    val bpnLegalEntity: String,
)