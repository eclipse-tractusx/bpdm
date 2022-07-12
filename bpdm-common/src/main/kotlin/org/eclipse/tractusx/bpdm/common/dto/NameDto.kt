package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.NameType

@Schema(name = "Name", description = "Name record for a business partner")
data class NameDto(
    @Schema(description = "Full name")
    val value: String,
    @Schema(description = "Abbreviated name or shorthand")
    val shortName: String?,
    @Schema(description = "Type of specified name", defaultValue = "OTHER")
    val type: NameType = NameType.OTHER,
    @Schema(description = "Language in which the name is specified", defaultValue = "undefined")
    val language: LanguageCode = LanguageCode.undefined
)