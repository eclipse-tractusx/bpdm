package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.AddressType
import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Address Response", description = "Localized address record of a business partner")
data class AddressResponse (
    val uuid: UUID,
    @Schema(description = "Language and character set the address is written in")
    val version: AddressVersionResponse,
    @Schema(description = "Entity which is in care of this address")
    val careOf: String?,
    @Schema(description = "Contexts of this address")
    val contexts: Collection<String>,
    @Schema(description = "Address country")
    val country: TypeKeyNameDto<CountryCode>,
    @ArraySchema(arraySchema = Schema(description = "Areas such as country region and county"))
    val administrativeAreas: Collection<AdministrativeAreaResponse>,
    @ArraySchema(arraySchema = Schema(description = "Postcodes assigned to this address"))
    val postCodes: Collection<PostCodeResponse>,
    @ArraySchema(arraySchema = Schema(description = "Localities such as city, block and quarter"))
    val localities: Collection<LocalityResponse>,
    @ArraySchema(arraySchema = Schema(description = "Thoroughfares such as street, zone and square"))
    val thoroughfares: Collection<ThoroughfareResponse>,
    @ArraySchema(arraySchema = Schema(description = "Premises such as building, level and room"))
    val premises: Collection<PremiseResponse>,
    @ArraySchema(arraySchema = Schema(description = "Delivery points for post"))
    val postalDeliveryPoints: Collection<PostalDeliveryPointResponse>,
    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto?,
    @ArraySchema(arraySchema = Schema(description = "Types of this address"))
    val types: Collection<TypeKeyNameUrlDto<AddressType>>
    )