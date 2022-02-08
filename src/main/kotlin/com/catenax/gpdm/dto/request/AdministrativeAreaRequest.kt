package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.AdministrativeAreaType

data class AdministrativeAreaRequest (
    val value: String,
    val shortName: String?,
    val fipsCode: String?,
    val type: AdministrativeAreaType
        )