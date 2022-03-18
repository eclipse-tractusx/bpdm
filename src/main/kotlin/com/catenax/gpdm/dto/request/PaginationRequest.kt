package com.catenax.gpdm.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Schema(name = "Pagination Request", description = "Defines pagination information for requesting collection results")
data class PaginationRequest (
    @field:Parameter(
        description = "Number of page to get results from", schema =
        Schema(defaultValue = "0"))
    @field:PositiveOrZero
    val page: Int=0,
    @field:Parameter(description = "Size of each page", schema =
    Schema(defaultValue = "10"))
    @field:Min(0)
    @field:Max(100)
    val size: Int=10
        )