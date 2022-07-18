package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressRequest

@Schema(name = "Site Request", description = "New site record")
data class SiteRequest(
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @Schema(description = "Site name")
    val name: String,
    @ArraySchema(arraySchema = Schema(description = "Addresses the site is located at", required = false))
    val addresses: Collection<AddressRequest> = emptyList()
)