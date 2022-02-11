package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.PremiseType

data class PremiseRequest (
    val value: String,
    val shortName: String?,
    val number: String?,
    val type: PremiseType = PremiseType.OTHER
        )