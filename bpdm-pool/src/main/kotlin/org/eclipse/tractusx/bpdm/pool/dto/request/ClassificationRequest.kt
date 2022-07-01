package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.entity.ClassificationType

@Schema(name = "Classification Request", description = "New classification record for a business partner")
data class ClassificationRequest (
        @Schema(description = "Name of the classification")
        val value: String,
        @Schema(description = "Identifying code of the classification, if applicable")
        val code: String?,
        @Schema(description = "Type of specified classification")
        val type: ClassificationType?
        )