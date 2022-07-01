package org.eclipse.tractusx.bpdm.pool.dto.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.boot.context.properties.ConstructorBinding

@Schema(name = "Address Properties Search Request", description = "Contains keywords used for searching in business partner properties")
data class AddressPropertiesSearchRequest @ConstructorBinding constructor(
    @field:Parameter(description = "Filter business partners by administrative area name")
    var administrativeArea: String? = null,
    @field:Parameter(description = "Filter business partners by postcode or postcodes")
    var postCode: String? = null,
    @field:Parameter(description = "Filter business partners by locality full denotation")
    var locality: String? = null,
    @field:Parameter(description = "Filter business partners by thoroughfare full denotation")
    var thoroughfare: String? = null,
    @field:Parameter(description = "Filter business partners by premise full denotation")
    var premise: String? = null,
    @field:Parameter(description = "Filter business partners by postal delivery point full denotation")
    var postalDeliveryPoint: String? = null
)
