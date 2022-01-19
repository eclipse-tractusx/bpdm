package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.NameType
import com.catenax.gpdm.entity.PremiseType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class PremiseDto (
    @JsonUnwrapped
    val nameComponent: BaseNamedDto,
    val type: PremiseType
        )