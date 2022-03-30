package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PostalDeliveryPointType
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Postal Delivery Point Response", description = "Postal delivery point record of an address")
data class PostalDeliveryPointResponse (
        @Schema(description = "Unique identifier for reference purposes")
        val uuid: UUID,
        @Schema(description = "Full denotation of the delivery point")
        val value: String,
        @Schema(description = "Abbreviation or shorthand of the locality's name")
        val shortName: String?,
        @Schema(description = "Number/code of the delivery point")
        val number: String?,
        @Schema(description = "Type of the specified delivery point")
        val type: TypeKeyNameUrlDto<PostalDeliveryPointType>,
        @Schema(description = "Language the delivery point is specified in")
        val language: TypeKeyNameDto<LanguageCode>
        )