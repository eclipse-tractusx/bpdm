package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.ThoroughfareType

@Schema(name = "Thoroughfare", description = "Thoroughfare record for an address such as street, square or industrial zone")
data class ThoroughfareDto(
        @Schema(description = "Full denotation of the thoroughfare")
        val value: String,
        @Schema(description = "Full name of the thoroughfare")
        val name: String? = null,
        @Schema(description = "Abbreviation or shorthand, if applicable")
        val shortName: String? = null,
        @Schema(description = "Thoroughfare number, if applicable")
        val number: String? = null,
        @Schema(description = "Direction information on the thoroughfare, if applicable")
        val direction: String? = null,
        @Schema(description = "Type of specified thoroughfare", defaultValue = "OTHER")
        var type: ThoroughfareType = ThoroughfareType.OTHER
)