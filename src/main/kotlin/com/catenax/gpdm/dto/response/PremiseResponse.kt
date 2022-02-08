package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PremiseType
import com.neovisionaries.i18n.LanguageCode

data class PremiseResponse (
    val value: String,
    val shortName: String?,
    val number: String?,
    val type: TypeKeyNameUrlDto<PremiseType>,
    val language: TypeKeyNameDto<LanguageCode>
        )