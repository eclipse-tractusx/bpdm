package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.entity.LocalityType

@Schema(name = "Locality Request", description = "New locality record for an address such as city, block or district")
data class LocalityRequest (
    @Schema(description = "Full name of the locality")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the locality's name, if applicable")
    val shortName: String?,
    @Schema(description = "Type of specified locality", defaultValue = "OTHER")
    val type: LocalityType = LocalityType.OTHER
        )