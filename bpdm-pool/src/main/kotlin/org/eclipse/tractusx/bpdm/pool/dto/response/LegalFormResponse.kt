package org.eclipse.tractusx.bpdm.pool.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeNameUrlDto

@Schema(name = "Legal Form Response", description = "Legal form a business partner can have")
data class LegalFormResponse (
    @Schema(description = "Unique key to be used for reference")
    val technicalKey: String,
    @Schema(description = "Full name of the legal form")
    val name: String,
    @Schema(description = "Link for further information on the legal form")
    val url: String? = null,
    @Schema(description = "Abbreviation of the legal form name")
    val mainAbbreviation: String? = null,
    @Schema(description = "Language in which the legal form is specified")
    val language: TypeKeyNameDto<LanguageCode>,
    @Schema(description = "Categories in which this legal form falls under")
    val categories: Collection<TypeNameUrlDto>  = emptyList()
    )