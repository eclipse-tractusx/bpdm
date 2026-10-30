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

package org.eclipse.tractusx.bpdm.gate.mapper.entity

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.AdditionalSiteDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.springframework.stereotype.Component

/**
 * Maps the values of a business partner input's content to the entity values that store them.
 */
@Component
class BusinessPartnerInputEntityMapper {

    /**
     * Returns the stored form of [identifier].
     */
    fun toEntity(identifier: BusinessPartnerInputContentParsed.Identifier): IdentifierDb =
        IdentifierDb(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody, BusinessPartnerType.GENERIC)

    /**
     * Returns the stored form of [state].
     */
    fun toEntity(state: BusinessPartnerInputContentParsed.State): StateDb =
        StateDb(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerTyp = state.businessPartnerType)

    /**
     * Returns the stored form of [additionalSite].
     */
    fun toEntity(additionalSite: BusinessPartnerValuesRaw.AdditionalSite): AdditionalSiteDb =
        AdditionalSiteDb(bpn = additionalSite.bpn, name = additionalSite.name)

    /**
     * Returns the stored form of the physical postal [address].
     */
    fun toEntity(address: BusinessPartnerValuesRaw.PhysicalPostalAddress): PhysicalPostalAddressDb =
        PhysicalPostalAddressDb(
            geographicCoordinates = address.geographicCoordinates?.let(::toEntity),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postalCode,
            city = address.city,
            district = address.district,
            street = address.street?.let(::toEntity),
            companyPostalCode = address.companyPostalCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    /**
     * Returns the stored form of the alternative postal [address].
     */
    fun toEntity(address: BusinessPartnerValuesRaw.AlternativePostalAddress): AlternativePostalAddressDb =
        AlternativePostalAddressDb(
            geographicCoordinates = address.geographicCoordinates?.let(::toEntity),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            postalCode = address.postalCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceQualifier = address.deliveryServiceQualifier,
            deliveryServiceNumber = address.deliveryServiceNumber
        )

    /**
     * Returns the stored form of a script variant's physical [address].
     */
    fun toEntity(address: BusinessPartnerValuesRaw.PhysicalAddressScriptVariant): PhysicalPostalAddressScriptVariantDb =
        PhysicalPostalAddressScriptVariantDb(
            city = address.city,
            district = address.district,
            street = with(address.street) {
                StreetScriptVariantDb(
                    name = name,
                    direction = direction,
                    namePrefix = namePrefix,
                    additionalNamePrefix = additionalNamePrefix,
                    nameSuffix = nameSuffix,
                    additionalNameSuffix = additionalNameSuffix
                )
            },
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door
        )

    /**
     * Returns the stored alternative address of a script variant located in [city].
     */
    fun toAlternativeAddressScriptVariant(city: String): AlternativePostalAddressScriptVariantDb =
        AlternativePostalAddressScriptVariantDb(city = city)

    private fun toEntity(street: StreetRaw) =
        StreetDb(
            name = street.name,
            houseNumber = street.houseNumber,
            houseNumberSupplement = street.houseNumberSupplement,
            milestone = street.milestone,
            direction = street.direction,
            namePrefix = street.namePrefix,
            additionalNamePrefix = street.additionalNamePrefix,
            nameSuffix = street.nameSuffix,
            additionalNameSuffix = street.additionalNameSuffix
        )

    private fun toEntity(coordinate: GeoCoordinateRaw) =
        GeographicCoordinateDb(latitude = coordinate.latitude, longitude = coordinate.longitude, altitude = coordinate.altitude)
}
