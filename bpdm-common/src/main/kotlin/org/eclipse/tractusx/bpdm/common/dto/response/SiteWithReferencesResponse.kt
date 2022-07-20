package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Site With References Response", description = "Site with legal entity reference.")
data class SiteWithReferencesResponse(
    val site: SiteResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
)