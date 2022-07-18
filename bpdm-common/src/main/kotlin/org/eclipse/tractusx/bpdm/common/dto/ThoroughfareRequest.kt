package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.ThoroughfareType

@Schema(name = "Thoroughfare Request", description = "New thoroughfare record for an address such as street, square or industrial zone")
data class ThoroughfareRequest (
        @Schema(description = "Full denotation of the thoroughfare")
        val value: String,
        @Schema(description = "Full name of the thoroughfare")
        val name: String?,
        @Schema(description = "Abbreviation or shorthand, if applicable")
        val shortName: String?,
        @Schema(description = "Thoroughfare number, if applicable")
        val number: String?,
        @Schema(description = "Direction information on the thoroughfare, if applicable")
        val direction: String?,
        @Schema(description = "Type of specified thoroughfare", defaultValue = "OTHER")
        var type: ThoroughfareType = ThoroughfareType.OTHER
        )