package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.PremiseType

@Schema(name = "Premise Request", description = "New premise record for an address such as building, room or floor")
data class PremiseRequest (
    @Schema(description = "Full denotation of the premise")
    val value: String,
    @Schema(description = "Abbreviation or shorthand, if applicable")
    val shortName: String? = null,
    @Schema(description = "Premise number, if applicable")
    val number: String? = null,
    @Schema(description = "Type of specified premise", defaultValue = "OTHER")
    val type: PremiseType = PremiseType.OTHER
)