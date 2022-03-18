package com.catenax.gpdm.dto.request

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.web.bind.annotation.RequestParam

@Schema(name = "Address Search Request", description = "Search request for addresses with keywords for address properties")
data class AddressSearchRequest @ConstructorBinding constructor(
    @Schema(description = "Which area, region or county")
    var administrativeArea: String? = null,
    @Schema(description = "Which postcode or postcodes")
    var postCode: String? = null,
    @Schema(description = "Which city, block or quarter")
    var locality: String? = null,
    @Schema(description = "Which street, zone or square")
    var thoroughfare: String? = null,
    @Schema(description = "Which building, level or room")
    var premise: String? = null,
    @Schema(description = "Which postal delivery point")
    var postalDeliveryPoint: String? = null
)
