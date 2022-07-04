package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConstructorBinding

@Schema(name = "Business Partner Properties Search Request", description = "Contains keywords used for searching in business partner properties")
data class BusinessPartnerPropertiesSearchRequest @ConstructorBinding constructor(
    @field:Parameter(description = "Filter business partners by name")
    val name: String?,
    @field:Parameter(description = "Filter business partners by legal form name")
    val legalForm: String?,
    @field:Parameter(description = "Filter business partners by status official denotation")
    val status: String?,
    @field:Parameter(description = "Filter business partners by classification denotation")
    val classification: String?,
)

