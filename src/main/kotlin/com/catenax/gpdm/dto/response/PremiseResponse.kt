package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PremiseType
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Premise Response", description = "Premise record of an address such as building, room or floor")
data class PremiseResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full denotation of the premise")
    val value: String,
    @Schema(description = "Abbreviation or shorthand")
    val shortName: String?,
    @Schema(description = "Premise number")
    val number: String?,
    @Schema(description = "Type of premise")
    val type: TypeKeyNameUrlDto<PremiseType>,
    @Schema(description = "Language the premise is specified in")
    val language: TypeKeyNameDto<LanguageCode>
        )