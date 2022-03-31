package com.catenax.gpdm.component.cdq.dto

data class AddressCdq(
    val id: String = "0",
    val externalId: String? = null,
    val cdqId: String? = null,
    val version: AddressVersionCdq? = null,
    val identifyingName: WrappedValueCdq? = null,
    val careOf: WrappedValueCdq? = null,
    val contexts: Collection<WrappedValueCdq> = emptyList(),
    val country: CountryCdq? = null,
    val administrativeAreas: Collection<AdministrativeAreaCdq> = emptyList(),
    val postCodes: Collection<PostCodeCdq> = emptyList(),
    val localities: Collection<LocalityCdq> = emptyList(),
    val thoroughfares: Collection<ThoroughfareCdq> = emptyList(),
    val premises: Collection<PremiseCdq> = emptyList(),
    val postalDeliveryPoints: Collection<PostalDeliveryPointCdq> = emptyList(),
    val geographicCoordinates: GeoCoordinatesCdq? = null,
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val metadataCdq: AddressMetadataCdq? = null
)
