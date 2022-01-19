package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.ClassificationType
import com.catenax.gpdm.entity.NameType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class ClassificationDto (
        @JsonUnwrapped
        val nameComponent: BaseNamedDto,
        val type: ClassificationType
        )