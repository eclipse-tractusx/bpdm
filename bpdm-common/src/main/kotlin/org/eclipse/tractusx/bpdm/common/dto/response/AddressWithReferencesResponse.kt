package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "Address With References Response", description = "Address with legal entity or site references. " +
            "Only one of either legal entity or site external id can be set for an address."
)
data class AddressWithReferencesResponse(
    val address: AddressResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
    @Schema(description = "External id of the related site")
    val siteExternalId: String?
)