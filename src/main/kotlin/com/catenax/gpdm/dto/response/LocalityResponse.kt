package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.LocalityType
import com.neovisionaries.i18n.LanguageCode

data class LocalityResponse (
    val value: String,
    val shortName: String?,
    val type: TypeKeyNameUrlDto<LocalityType>,
    val language: TypeKeyNameDto<LanguageCode>
        )