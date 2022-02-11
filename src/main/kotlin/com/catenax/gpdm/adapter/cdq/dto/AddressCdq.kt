package com.catenax.gpdm.adapter.cdq.dto

data class AddressCdq(
    val id: String,
    val externalId: String?,
    val cdqId: String?,
    val version: AddressVersionCdq?,
    val identifyingName: WrappedValueCdq?,
    val careOf: WrappedValueCdq?,
    val contexts: Collection<WrappedValueCdq> = emptyList(),
    val country: CountryCdq?,
    val administrativeAreas: Collection<AdministrativeAreaCdq> = emptyList(),
    val postCodes: Collection<PostCodeCdq> = emptyList(),
    val localities: Collection<LocalityCdq> = emptyList(),
    val thoroughfares: Collection<ThoroughfareCdq> = emptyList(),
    val premises: Collection<PremiseCdq> = emptyList(),
    val postalDeliveryPoints: Collection<PostalDeliveryPointCdq> = emptyList(),
    val geographicCoordinates: GeoCoordinatesCdq?,
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val metadataCdq: AddressMetadataCdq?
)
