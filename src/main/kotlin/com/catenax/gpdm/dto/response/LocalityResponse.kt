package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.LocalityType
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Locality Response", description = "Locality record of an address such as city, block or district")
data class LocalityResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full name of the locality")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the locality's name")
    val shortName: String?,
    @Schema(description = "Type of locality")
    val type: TypeKeyNameUrlDto<LocalityType>,
    @Schema(description = "Language the locality is specified in")
    val language: TypeKeyNameDto<LanguageCode>
        )