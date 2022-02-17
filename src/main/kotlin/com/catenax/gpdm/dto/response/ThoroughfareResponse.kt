package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.ThoroughfareType
import com.neovisionaries.i18n.LanguageCode
import java.util.*


class ThoroughfareResponse (
        val uuid: UUID,
        val value: String,
        val name: String?,
        val shortName: String?,
        val number: String?,
        val direction: String?,
        var type: TypeKeyNameUrlDto<ThoroughfareType>,
        var language: TypeKeyNameDto<LanguageCode>
        )