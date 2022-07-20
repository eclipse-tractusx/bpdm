package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.PostCodeType
import java.util.*

@Schema(name = "Postcode Response", description = "Postcode record of an address")
data class PostCodeResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full postcode denotation")
    val value: String,
    @Schema(description = "Type of specified postcode")
    val type: TypeKeyNameUrlDto<PostCodeType>
        )