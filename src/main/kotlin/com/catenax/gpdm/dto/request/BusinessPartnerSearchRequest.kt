package com.catenax.gpdm.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springdoc.api.annotations.ParameterObject
import org.springframework.boot.context.properties.ConstructorBinding

@Schema(name = "Business Partner Search Request", description = "Searching request for business partners by keywords in properties")
data class BusinessPartnerSearchRequest @ConstructorBinding constructor(
    @Parameter(description = "Which business partner name")
    val name: String?,
    @Parameter(description = "Which legal form name")
    val legalForm: String?,
    @Parameter(description = "Which business status designation")
    val status: String?,
    @ParameterObject
    var address: AddressSearchRequest?,
    @Parameter(description = "Which business classification")
    val classification: String?,
)

