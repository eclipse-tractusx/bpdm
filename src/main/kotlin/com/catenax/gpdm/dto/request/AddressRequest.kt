package com.catenax.gpdm.dto.request

import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.entity.AddressType
import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Address Request", description = "New localized address record for a business partner")
data class AddressRequest (
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @Schema(description = "Language and character set the address is written in")
    val version: AddressVersionRequest = AddressVersionRequest(),
    @Schema(description = "Entity which is in care of this address")
    val careOf: String? = null,
    @Schema(description = "Contexts of this address")
    val contexts: Collection<String> = emptyList(),
    @Schema(description = "Address country", defaultValue = "UNDEFINED")
    val country: CountryCode = CountryCode.UNDEFINED,
    @ArraySchema(arraySchema = Schema(description = "Area such as country region or county", defaultValue = "[]"))
    val administrativeAreas: Collection<AdministrativeAreaRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postcodes assigned to this address", defaultValue = "[]"))
    val postCodes: Collection<PostCodeRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "City, block and/or quarter", defaultValue = "[]"))
    val localities: Collection<LocalityRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Street, zone and/or square", defaultValue = "[]"))
    val thoroughfares: Collection<ThoroughfareRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Building, level and/or room", defaultValue = "[]"))
    val premises: Collection<PremiseRequest> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postal delivery points", defaultValue = "[]"))
    val postalDeliveryPoints: Collection<PostalDeliveryPointRequest> = emptyList(),
    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @ArraySchema(arraySchema = Schema(description = "Type of address", defaultValue = "[]"))
    val types: Collection<AddressType> = emptyList()
)