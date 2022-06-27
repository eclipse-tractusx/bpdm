package com.catenax.gpdm.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Identifiers Search Request", description = "Contains identifiers to search business partners by")
data class IdentifiersSearchRequest(
    @Schema(description = "Technical key of the type to which the identifiers belongs to")
    val idType: String,
    @Schema(description = "Values of the identifiers")
    val idValues: List<String>
)