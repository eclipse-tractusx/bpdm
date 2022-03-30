package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.BusinessStatusType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@Schema(name = "Business Status Response", description = "Status of a business partner")
data class BusinessStatusResponse (
        @Schema(description = "Unique identifier for reference purposes")
        val uuid: UUID,
        @Schema(description = "Exact, official denotation of the status")
        val officialDenotation: String,
        @Schema(description = "Since when the status is/was valid")
        val validFrom: LocalDateTime,
        @Schema(description = "Until the status was valid, if applicable")
        val validUntil: LocalDateTime?,
        @Schema(description = "The type of this status")
        val type: TypeKeyNameUrlDto<BusinessStatusType>
        )