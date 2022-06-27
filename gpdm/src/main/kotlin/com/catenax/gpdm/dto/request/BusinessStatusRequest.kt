package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.BusinessStatusType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "Business Status Request", description = "New status record for a business partner")
data class BusinessStatusRequest (
    @Schema(description = "Exact, official denotation of the status")
    val officialDenotation: String,
    @Schema(description = "Since when the status is/was valid")
    val validFrom: LocalDateTime,
    @Schema(description = "Until the status was valid, if applicable")
    val validUntil: LocalDateTime?,
    @Schema(description = "The type of this specified status")
    val type: BusinessStatusType
        )