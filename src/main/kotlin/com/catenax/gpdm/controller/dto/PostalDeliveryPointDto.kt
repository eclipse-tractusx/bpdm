package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.PostalDeliveryPointType
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class PostalDeliveryPointDto (
        @JsonUnwrapped
        val nameComponent: BaseNamedDto,
        val type: PostalDeliveryPointType
        )