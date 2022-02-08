package com.catenax.gpdm.dto.request

import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.neovisionaries.i18n.LanguageCode

data class LegalFormRequest(
    val technicalKey: String,
    val name: String,
    val url: String?,
    val mainAbbreviation: String?,
    val language: LanguageCode,
    val category: Collection<TypeNameUrlDto>
)
