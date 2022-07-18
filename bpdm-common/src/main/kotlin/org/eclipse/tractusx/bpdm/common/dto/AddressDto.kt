package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.CountryCode
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.AddressType

@Schema(name = "Address Request", description = "New localized address record for a business partner")
data class AddressDto(
    @Schema(description = "Business Partner Number")
    val bpn: String?,
    @Schema(description = "Language and character set the address is written in")
    val version: AddressVersionDto = AddressVersionDto(),
    @Schema(description = "Entity which is in care of this address")
    val careOf: String? = null,
    @Schema(description = "Contexts of this address")
    val contexts: Collection<String> = emptyList(),
    @Schema(description = "Address country", defaultValue = "UNDEFINED")
    val country: CountryCode = CountryCode.UNDEFINED,
    @ArraySchema(arraySchema = Schema(description = "Area such as country region or county", defaultValue = "[]"))
    val administrativeAreas: Collection<AdministrativeAreaDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postcodes assigned to this address", defaultValue = "[]"))
    val postCodes: Collection<PostCodeDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "City, block and/or quarter", defaultValue = "[]"))
    val localities: Collection<LocalityDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Street, zone and/or square", defaultValue = "[]"))
    val thoroughfares: Collection<ThoroughfareDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Building, level and/or room", defaultValue = "[]"))
    val premises: Collection<PremiseDto> = emptyList(),
    @ArraySchema(arraySchema = Schema(description = "Postal delivery points", defaultValue = "[]"))
    val postalDeliveryPoints: Collection<PostalDeliveryPointDto> = emptyList(),
    @Schema(description = "Geographic coordinates to find this location")
    val geographicCoordinates: GeoCoordinateDto? = null,
    @ArraySchema(arraySchema = Schema(description = "Type of address", defaultValue = "[]"))
    val types: Collection<AddressType> = emptyList()
)