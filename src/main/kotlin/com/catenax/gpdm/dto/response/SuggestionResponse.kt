package com.catenax.gpdm.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Suggestion Response", description = "Shows a ranked suggestion based on a given search text")
data class SuggestionResponse(
    @Schema(description = "The suggestion text")
    val suggestion: String,
    @Schema(description = "Relative relevancy score indicating quality of the match, higher is better")
    val relevancy: Float
)
