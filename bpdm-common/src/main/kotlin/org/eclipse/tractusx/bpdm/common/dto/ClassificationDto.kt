package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.ClassificationType

@Schema(name = "Classification", description = "Classification record for a business partner")
data class ClassificationDto(
        @Schema(description = "Name of the classification")
        val value: String,
        @Schema(description = "Identifying code of the classification, if applicable")
        val code: String?,
        @Schema(description = "Type of specified classification")
        val type: ClassificationType?
)