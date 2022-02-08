package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.NameType
import com.neovisionaries.i18n.LanguageCode

data class NameResponse (
    val value: String,
    val shortName: String?,
    val type: TypeKeyNameUrlDto<NameType>,
    val language: TypeKeyNameDto<LanguageCode>
        )