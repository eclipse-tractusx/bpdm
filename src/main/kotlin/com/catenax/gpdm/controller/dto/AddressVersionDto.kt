package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.CharacterSet
import com.neovisionaries.i18n.LanguageCode

data class AddressVersionDto (
    val characterSet: CharacterSet,
    val languageCode: LanguageCode
        )