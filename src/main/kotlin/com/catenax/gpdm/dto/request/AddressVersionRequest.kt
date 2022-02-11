package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.CharacterSet
import com.neovisionaries.i18n.LanguageCode

data class AddressVersionRequest (
    val characterSet: CharacterSet = CharacterSet.UNDEFINED,
    val language: LanguageCode = LanguageCode.undefined
        )