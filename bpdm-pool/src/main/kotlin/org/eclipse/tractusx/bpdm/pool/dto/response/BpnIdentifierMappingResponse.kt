package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Bpn Identifier Mapping Response", description = "Mapping of Business Partner Number to identifier value")
data class BpnIdentifierMappingResponse(
    @Schema(description = "Value of the identifier")
    val idValue: String,
    @Schema(description = "Business Partner Number")
    val bpn: String
)