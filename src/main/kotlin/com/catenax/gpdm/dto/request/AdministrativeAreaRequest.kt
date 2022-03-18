package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.AdministrativeAreaType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Administrative Area Request", description = "New areas such as country regions or counties")
data class AdministrativeAreaRequest (
    @Schema(description = "Full name of the area")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the area")
    val shortName: String?,
    @Schema(description = "FIPS code if applicable")
    val fipsCode: String?,
    @Schema(description = "Type of specified area")
    val type: AdministrativeAreaType = AdministrativeAreaType.OTHER
        )