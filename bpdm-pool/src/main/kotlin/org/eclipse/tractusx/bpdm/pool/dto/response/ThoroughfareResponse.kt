package org.eclipse.tractusx.bpdm.pool.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.pool.entity.ThoroughfareType
import java.util.*

@Schema(name = "Thoroughfare Response", description = "Thoroughfare record of an address such as street, square or industrial zone")
class ThoroughfareResponse (
    @Schema(description = "Unique identifier for reference purposes")
        val uuid: UUID,
    @Schema(description = "Full denotation of the thoroughfare")
        val value: String,
    @Schema(description = "Full name of the thoroughfare")
        val name: String? = null,
    @Schema(description = "Abbreviation or shorthand")
        val shortName: String? = null,
    @Schema(description = "Thoroughfare number")
        val number: String? = null,
    @Schema(description = "Direction information on the thoroughfare")
        val direction: String? = null,
    @Schema(description = "Type of thoroughfare", defaultValue = "OTHER")
        var type: TypeKeyNameUrlDto<ThoroughfareType>,
    @Schema(description = "Language the thoroughfare is specified in")
        var language: TypeKeyNameDto<LanguageCode>
        )