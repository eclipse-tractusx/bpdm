package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Paginated collection of results")
data class PageResponse<T> (
    @Schema(description = "Total number of all results in all pages")
    val totalElements: Long,
    @Schema(description = "Total number pages")
    val totalPages: Int,
    @Schema(description = "Current page number")
    val page: Int,
    @Schema(description = "Number of results in the page")
    val contentSize: Int,
    @Schema(description = "Collection of results in the page")
    val content: Collection<T>
        )