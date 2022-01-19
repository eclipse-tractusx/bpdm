package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.PostCodeType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class PostCodeDto (
    @JsonUnwrapped
    val nameComponent: BaseNamedDto,
    val type: PostCodeType
        )