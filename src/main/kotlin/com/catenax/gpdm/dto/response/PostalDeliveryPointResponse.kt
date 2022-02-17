package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PostalDeliveryPointType
import com.neovisionaries.i18n.LanguageCode
import java.util.*

data class PostalDeliveryPointResponse (
        val uuid: UUID,
        val value: String,
        val shortName: String?,
        val number: String?,
        val type: TypeKeyNameUrlDto<PostalDeliveryPointType>,
        val language: TypeKeyNameDto<LanguageCode>
        )