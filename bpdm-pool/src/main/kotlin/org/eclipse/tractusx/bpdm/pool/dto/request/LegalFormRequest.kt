package org.eclipse.tractusx.bpdm.pool.dto.request

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto

@Schema(name = "Legal Form Request", description = "New legal form record to be referenced by business partners")
data class LegalFormRequest(
    @Schema(description = "Unique key to be used for reference")
    val technicalKey: String,
    @Schema(description = "Full name of the legal form")
    val name: String,
    @Schema(description = "Link for further information on the legal form")
    val url: String?,
    @Schema(description = "Abbreviation of the legal form name")
    val mainAbbreviation: String?,
    @Schema(description = "Language in which the legal form is specified", defaultValue = "undefined")
    val language: LanguageCode = LanguageCode.undefined,
    @Schema(description = "Categories in which this legal form falls under", defaultValue = "[]")
    val category: Collection<TypeNameUrlDto> = emptyList()
)
