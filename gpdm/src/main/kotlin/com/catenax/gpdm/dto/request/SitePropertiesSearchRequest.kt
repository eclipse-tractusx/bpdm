package com.catenax.gpdm.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConstructorBinding

@Schema(name = "Site Properties Search Request", description = "Contains keywords used for searching in site properties")
data class SitePropertiesSearchRequest @ConstructorBinding constructor(
    @field:Parameter(description = "Filter sites by name")
    val siteName: String?
)
