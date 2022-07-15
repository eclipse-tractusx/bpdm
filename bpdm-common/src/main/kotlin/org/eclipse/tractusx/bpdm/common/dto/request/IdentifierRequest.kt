package org.eclipse.tractusx.bpdm.common.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Identifier Request", description = "New identifier record for a business partner")
data class IdentifierRequest (
    @Schema(description = "Value of the identifier")
    val value: String,
    @Schema(description = "Technical key of the type to which this identifier belongs to")
    val type: String,
    @Schema(description = "Technical key of the body which issued this identifier")
    val issuingBody: String?,
    @Schema(description = "Technical key of the status this identifier has")
    val status: String?
        )