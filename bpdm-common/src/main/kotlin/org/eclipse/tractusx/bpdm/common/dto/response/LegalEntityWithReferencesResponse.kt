package org.eclipse.tractusx.bpdm.common.dto.response


import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Legal Entity With References Response", description = "Legal entity with references")
data class LegalEntityWithReferencesResponse(
    val legalEntityResponse: LegalEntityResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String
)