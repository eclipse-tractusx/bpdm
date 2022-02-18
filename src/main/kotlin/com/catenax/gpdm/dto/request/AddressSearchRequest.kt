package com.catenax.gpdm.dto.request

import com.fasterxml.jackson.annotation.JsonCreator
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.web.bind.annotation.RequestParam

data class AddressSearchRequest @ConstructorBinding constructor(
    var administrativeArea: String? = null,
    var postCode: String? = null,
    var locality: String? = null,
    var thoroughfare: String? = null,
    var premise: String? = null,
    var postalDeliveryPoint: String? = null
)
