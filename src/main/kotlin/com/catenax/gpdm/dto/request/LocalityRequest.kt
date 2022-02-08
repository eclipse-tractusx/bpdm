package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.LocalityType

data class LocalityRequest (
    val value: String,
    val shortName: String?,
    val type: LocalityType
        )