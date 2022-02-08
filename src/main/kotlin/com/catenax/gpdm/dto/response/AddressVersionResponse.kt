package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.entity.CharacterSet
import com.neovisionaries.i18n.LanguageCode

data class AddressVersionResponse (
    val characterSet: TypeKeyNameDto<CharacterSet>,
    val language: TypeKeyNameDto<LanguageCode>
        )