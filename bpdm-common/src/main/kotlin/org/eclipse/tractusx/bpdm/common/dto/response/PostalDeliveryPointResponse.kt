package org.eclipse.tractusx.bpdm.common.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.PostalDeliveryPointType

@Schema(name = "Postal Delivery Point Response", description = "Postal delivery point record of an address")
data class PostalDeliveryPointResponse (
    @Schema(description = "Full denotation of the delivery point")
        val value: String,
    @Schema(description = "Abbreviation or shorthand of the locality's name")
        val shortName: String? = null,
    @Schema(description = "Number/code of the delivery point")
        val number: String? = null,
    @Schema(description = "Type of the specified delivery point")
        val type: TypeKeyNameUrlDto<PostalDeliveryPointType>,
    @Schema(description = "Language the delivery point is specified in")
        val language: TypeKeyNameDto<LanguageCode>
        )