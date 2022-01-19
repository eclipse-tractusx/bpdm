package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.LocalityType
import com.catenax.gpdm.entity.NameType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class LocalityDto (
    @JsonUnwrapped
    val nameComponent: BaseNamedDto,
    val type: LocalityType
        )