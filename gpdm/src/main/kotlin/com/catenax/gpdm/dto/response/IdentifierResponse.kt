package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Identifier Response", description = "Identifier record of a business partner")
data class IdentifierResponse (
        @Schema(description = "Unique identifier for reference purposes")
        val uuid: UUID,
        @Schema(description = "Value of the identifier")
        val value: String,
        @Schema(description = "Type of the identifier")
        val type: TypeKeyNameUrlDto<String>,
        @Schema(description = "Body which issued the identifier")
        val issuingBody:  TypeKeyNameUrlDto<String>? = null,
        @Schema(description = "Status of the identifier")
        val status: TypeKeyNameDto<String>? = null
        )