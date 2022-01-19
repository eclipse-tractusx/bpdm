package com.catenax.gpdm.controller.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped

data class LegalFormDto (
    @JsonUnwrapped
    val nameComponent: BaseNamedDto,
    val type: String
        )