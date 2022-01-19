package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.AdministrativeAreaCode
import com.catenax.gpdm.entity.AdministrativeAreaType

data class AdministrativeAreaDto (
    val name: String,
    val codes: Collection<AdministrativeAreaCodeDto>,
    val type: AdministrativeAreaType
        )