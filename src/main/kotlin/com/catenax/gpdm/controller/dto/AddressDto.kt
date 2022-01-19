package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.AddressType
import com.catenax.gpdm.entity.AddressVersion
import com.catenax.gpdm.entity.PostCode
import com.catenax.gpdm.entity.PostalDeliveryPoint
import com.neovisionaries.i18n.CountryCode

data class AddressDto (
    val identifiers: Collection<IdentifierDto>,
    val careOf: BaseNamedDto?,
    val countryCode: CountryCode,
    val administrativeAreas: Collection<AdministrativeAreaDto>,
    val postCodes: Collection<PostCodeDto>,
    val localities: Collection<LocalityDto>,
    val thoroughfares: Collection<ThoroughfareDto>,
    val premises: Collection<PremiseDto>,
    val postalDeliveryPoints: Collection<PostalDeliveryPointDto>,
    val type: AddressType,
    val versions: Collection<AddressVersionDto>
        )