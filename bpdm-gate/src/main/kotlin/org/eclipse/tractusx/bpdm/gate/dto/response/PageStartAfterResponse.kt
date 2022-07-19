package org.eclipse.tractusx.bpdm.gate.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Page Response", description = "Paginated collection of results")
data class PageStartAfterResponse<T>(
    @Schema(description = "Total number of all results in all pages")
    val total: Int,
    @Schema(description = "Value to be used as startAfter in request for next page. Value is only sent if more data exists for a next page.")
    val nextStartAfter: String?,
    @Schema(description = "Collection of results in the page")
    val content: Collection<T>
)