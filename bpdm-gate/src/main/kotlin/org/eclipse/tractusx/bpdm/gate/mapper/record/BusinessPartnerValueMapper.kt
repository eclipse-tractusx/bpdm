/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.mapper.record

import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.value.Street
import org.springframework.stereotype.Component

/**
 * Maps the stored value parts of a business partner to their values, the same in every stage.
 */
@Component
class BusinessPartnerValueMapper {

    /**
     * Returns the value of a stored physical postal address.
     */
    fun toValue(address: PhysicalPostalAddressDb) =
        BusinessPartnerValues.PhysicalPostalAddress(
            geographicCoordinates = address.geographicCoordinates?.let(::toValue),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postalCode,
            city = address.city,
            district = address.district,
            street = address.street?.let(::toValue),
            companyPostalCode = address.companyPostalCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    /**
     * Returns the value of a stored alternative postal address.
     */
    fun toValue(address: AlternativePostalAddressDb) =
        BusinessPartnerValues.AlternativePostalAddress(
            geographicCoordinates = address.geographicCoordinates?.let(::toValue),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            postalCode = address.postalCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceQualifier = address.deliveryServiceQualifier,
            deliveryServiceNumber = address.deliveryServiceNumber
        )

    private fun toValue(street: StreetDb) =
        Street(
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            name = street.name,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction
        )

    private fun toValue(coordinate: GeographicCoordinateDb) =
        GeoCoordinate(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    /**
     * Returns the value of a stored script variant.
     */
    fun toValue(scriptVariant: BusinessPartnerScriptVariantDb) =
        BusinessPartnerValues.ScriptVariant(
            scriptCode = scriptVariant.scriptCode,
            nameParts = scriptVariant.nameParts.toList(),
            legalName = scriptVariant.legalName,
            shortName = scriptVariant.shortName,
            siteName = scriptVariant.siteName,
            addressName = scriptVariant.addressName,
            physicalAddress = toValue(scriptVariant.physicalAddress),
            alternativeAddressCity = scriptVariant.alternativeAddress?.city
        )

    // An all-empty embeddable is read back as null, while a request always states the physical script variant address.
    private fun toValue(address: PhysicalPostalAddressScriptVariantDb?) =
        BusinessPartnerValues.PhysicalAddressScriptVariant(
            city = address?.city,
            district = address?.district,
            street = toValue(address?.street),
            industrialZone = address?.industrialZone,
            building = address?.building,
            floor = address?.floor,
            door = address?.door
        )

    private fun toValue(street: StreetScriptVariantDb?) =
        BusinessPartnerValues.StreetScriptVariant(
            name = street?.name,
            direction = street?.direction,
            namePrefix = street?.namePrefix,
            additionalNamePrefix = street?.additionalNamePrefix,
            nameSuffix = street?.nameSuffix,
            additionalNameSuffix = street?.additionalNameSuffix
        )
}
