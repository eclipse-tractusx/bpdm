package org.eclipse.tractusx.bpdm.common.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.AdministrativeAreaType

@Schema(name = "Administrative Area Response", description = "Area of an address such as country region or county")
data class AdministrativeAreaResponse (
    @Schema(description = "Full name of the area")
    val value: String,
    @Schema(description = "Abbreviation or shorthand of the area")
    val shortName: String? = null,
    @Schema(description = "FIPS code if applicable")
    val fipsCode: String? = null,
    @Schema(description = "Type of specified area")
    val type: TypeKeyNameUrlDto<AdministrativeAreaType>,
    @Schema(description = "Language the area is specified in")
    val language: TypeKeyNameDto<LanguageCode>
        )