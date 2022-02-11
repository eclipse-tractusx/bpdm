package com.catenax.gpdm.dto.request

import com.catenax.gpdm.dto.GeoCoordinateDto
import com.catenax.gpdm.entity.AddressType
import com.neovisionaries.i18n.CountryCode

data class AddressRequest (
    val version: AddressVersionRequest,
    val careOf: String?,
    val contexts: Collection<String> = emptyList(),
    val country: CountryCode = CountryCode.UNDEFINED,
    val administrativeAreas: Collection<AdministrativeAreaRequest> = emptyList(),
    val postCodes: Collection<PostCodeRequest> = emptyList(),
    val localities: Collection<LocalityRequest> = emptyList(),
    val thoroughfares: Collection<ThoroughfareRequest> = emptyList(),
    val premises: Collection<PremiseRequest> = emptyList(),
    val postalDeliveryPoints: Collection<PostalDeliveryPointRequest> = emptyList(),
    val geographicCoordinates: GeoCoordinateDto?,
    val types: Collection<AddressType> = emptyList()
        )