package com.catenax.gpdm.controller.dto

import javax.validation.constraints.Max
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

data class PaginationRequest (
    @PositiveOrZero
    val page: Int=0,
    @Positive
    @Max(100)
    val size: Int=1
        )