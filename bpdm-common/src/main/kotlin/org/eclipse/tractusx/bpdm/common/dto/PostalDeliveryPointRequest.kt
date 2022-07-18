package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.PostalDeliveryPointType

@Schema(name = "Postal Delivery Point Request", description = "New postal delivery point record for an address")
data class PostalDeliveryPointRequest (
    @Schema(description = "Full name of the delivery point")
    val value: String,
    @Schema(description = "Abbreviation or shorthand, if applicable")
    val shortName: String?,
    @Schema(description = "Number/code of the delivery point, if applicable")
    val number: String?,
    @Schema(description = "Type of the specified delivery point", defaultValue = "OTHER")
    val type: PostalDeliveryPointType = PostalDeliveryPointType.OTHER
        )