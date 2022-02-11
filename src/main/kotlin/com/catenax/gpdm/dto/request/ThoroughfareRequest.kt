package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.ThoroughfareType

data class ThoroughfareRequest (
        val value: String,
        val name: String?,
        val shortName: String?,
        val number: String?,
        val direction: String?,
        var type: ThoroughfareType = ThoroughfareType.OTHER
        )