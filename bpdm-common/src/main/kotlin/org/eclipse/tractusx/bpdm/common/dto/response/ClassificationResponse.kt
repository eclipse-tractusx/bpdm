package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto

@Schema(name = "Classification Response", description = "Classification record of a business partner")
data class ClassificationResponse (
        val value: String,
        val code: String? = null,
        val type: TypeNameUrlDto? = null
        )