package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.PremiseType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Premise Request", description = "New premise record for an address such as building, room or floor")
data class PremiseRequest (
    @Schema(description = "Full denotation of the premise")
    val value: String,
    @Schema(description = "Abbreviation or shorthand, if applicable")
    val shortName: String?,
    @Schema(description = "Premise number, if applicable")
    val number: String?,
    @Schema(description = "Type of specified premise", defaultValue = "OTHER")
    val type: PremiseType = PremiseType.OTHER
        )