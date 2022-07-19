package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeNameUrlDto
import java.util.*

@Schema(name = "Classification Response", description = "Classification record of a business partner")
data class ClassificationResponse (
        val uuid: UUID,
        val value: String,
        val code: String? = null,
        val type: TypeNameUrlDto? = null
        )