package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeNameUrlDto
import com.neovisionaries.i18n.LanguageCode

data class LegalFormResponse (
    val technicalKey: String,
    val name: String,
    val url: String?,
    val mainAbbreviation: String?,
    val language: TypeKeyNameDto<LanguageCode>,
    val category: Collection<TypeNameUrlDto>
    )