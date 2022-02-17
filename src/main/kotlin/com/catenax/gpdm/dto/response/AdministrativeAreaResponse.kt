package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.AdministrativeAreaType
import com.neovisionaries.i18n.LanguageCode
import java.util.*

data class AdministrativeAreaResponse (
    val uuid: UUID,
    val value: String,
    val shortName: String?,
    val fipsCode: String?,
    val type: TypeKeyNameUrlDto<AdministrativeAreaType>,
    val language: TypeKeyNameDto<LanguageCode>
        )