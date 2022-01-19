package com.catenax.gpdm.controller.dto


import com.catenax.gpdm.entity.ThoroughfareType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class ThoroughfareDto (
        @JsonUnwrapped
        val nameComponent: BaseNamedDto,
        val type: ThoroughfareType
        )