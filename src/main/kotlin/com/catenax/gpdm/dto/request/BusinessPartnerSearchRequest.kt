package com.catenax.gpdm.dto.request

import org.springframework.boot.context.properties.ConstructorBinding

data class BusinessPartnerSearchRequest @ConstructorBinding constructor(
    val name: String?,
    val legalForm: String?,
    val status: String?,
    var address: AddressSearchRequest?,
    val classification: String?,
)

