package org.eclipse.tractusx.bpdm.gate.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Schema(name = "Pagination Start After Request", description = "Defines pagination information for requesting collection results")
data class PaginationStartAfterRequest(
    @field:Parameter(
        description = "Value used to indicate which page to retrieve. When this value is not provided, the first page is returned." +
                "The nextStartAfter value from the response can then be used to request subsequent pages."
    )
    val startAfter: String?,
    @field:Parameter(
        description = "Size of each page", schema =
        Schema(defaultValue = "10")
    )
    @field:Min(1)
    @field:Max(100)
    val limit: Int = 10
)