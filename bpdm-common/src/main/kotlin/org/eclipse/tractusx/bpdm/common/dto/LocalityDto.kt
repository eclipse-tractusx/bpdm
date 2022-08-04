package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.LocalityType

@Schema(name = "Locality", description = "Locality record for an address such as city, block or district")
data class LocalityDto(
    @Schema(description = "Full name of the locality")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the locality's name, if applicable")
    val shortName: String? = null,
    @Schema(description = "Type of specified locality", defaultValue = "OTHER")
    val type: LocalityType = LocalityType.OTHER
)