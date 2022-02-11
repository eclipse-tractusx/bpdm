package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.NameType
import com.neovisionaries.i18n.LanguageCode

data class NameRequest (
        val value: String,
        val shortName: String?,
        val type: NameType = NameType.OTHER,
        val language: LanguageCode = LanguageCode.undefined
        )