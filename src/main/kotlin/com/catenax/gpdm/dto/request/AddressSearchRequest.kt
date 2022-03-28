package com.catenax.gpdm.dto.request

import org.springframework.boot.context.properties.ConstructorBinding

data class AddressSearchRequest @ConstructorBinding constructor(
    var administrativeArea: String? = null,
    var postCode: String? = null,
    var locality: String? = null,
    var thoroughfare: String? = null,
    var premise: String? = null,
    var postalDeliveryPoint: String? = null
)
