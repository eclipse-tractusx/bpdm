package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.NameType
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Name Response", description = "Name record of a business partner")
data class NameResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full name")
    val value: String,
    @Schema(description = "Abbreviated name or shorthand")
    val shortName: String?,
    @Schema(description = "Type of name")
    val type: TypeKeyNameUrlDto<NameType>,
    @Schema(description = "Language in which the name is specified")
    val language: TypeKeyNameDto<LanguageCode>
        )