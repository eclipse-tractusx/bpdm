package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Legal Entity With References", description = "Legal entity with external id")
data class LegalEntityWithReferencesDto(
    @Schema(description = "ID the record has in the external system where the record originates from", required = true)
    val externalId: String,
    val legalEntity: LegalEntityDto
)
