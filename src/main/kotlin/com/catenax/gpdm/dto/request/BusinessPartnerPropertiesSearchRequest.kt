package com.catenax.gpdm.dto.request

import org.springframework.boot.context.properties.ConstructorBinding

data class BusinessPartnerPropertiesSearchRequest @ConstructorBinding constructor(
    val name: String?,
    val legalForm: String?,
    val status: String?,
    val classification: String?,
)

