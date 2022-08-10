package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AddressBpnResponse

@Schema(name = "Address With Reference Response", description = "Address with bpn references")
data class AddressWithReferenceResponse(
    val address: AddressBpnResponse,
    @Schema(description = "Business Partner Number of the related legal entity")
    val bpnLegalEntity: String?,
    @Schema(description = "Business Partner Number of the related site")
    val bpnSite: String?,
)