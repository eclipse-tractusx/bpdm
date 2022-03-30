package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.AdministrativeAreaType
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Administrative Area Response", description = "Area of an address such as country region or county")
data class AdministrativeAreaResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full name of the area")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the area")
    val shortName: String?,
    @Schema(description = "FIPS code if applicable")
    val fipsCode: String?,
    @Schema(description = "Type of specified area")
    val type: TypeKeyNameUrlDto<AdministrativeAreaType>,
    @Schema(description = "Language the area is specified in")
    val language: TypeKeyNameDto<LanguageCode>
        )