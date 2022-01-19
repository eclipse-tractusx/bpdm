package com.catenax.gpdm.controller.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped

data class IdentifierDto (
        @JsonUnwrapped
        val nameComponent: BaseNamedDto,
        val type: String,
        val registration: RegistrationDto?
        )