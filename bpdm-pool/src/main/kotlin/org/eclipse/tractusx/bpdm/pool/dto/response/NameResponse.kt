package org.eclipse.tractusx.bpdm.pool.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.entity.NameType
import java.util.*

@Schema(name = "Name Response", description = "Name record of a business partner")
data class NameResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Full name")
    val value: String,
    @Schema(description = "Abbreviated name or shorthand")
    val shortName: String? = null,
    @Schema(description = "Type of name")
    val type: TypeKeyNameUrlDto<NameType>,
    @Schema(description = "Language in which the name is specified")
    val language: TypeKeyNameDto<LanguageCode>
        )