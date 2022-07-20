package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.BusinessStatusType
import java.time.LocalDateTime

@Schema(name = "Business Status Response", description = "Status of a business partner")
data class BusinessStatusResponse (
        @Schema(description = "Exact, official denotation of the status")
        val officialDenotation: String,
        @Schema(description = "Since when the status is/was valid")
        val validFrom: LocalDateTime,
        @Schema(description = "Until the status was valid, if applicable")
        val validUntil: LocalDateTime? = null,
        @Schema(description = "The type of this status")
        val type: TypeKeyNameUrlDto<BusinessStatusType>
        )