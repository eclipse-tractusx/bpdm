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
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputValues
import org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.value.Street
import org.springframework.stereotype.Component

/**
 * Maps a stored business partner input to the content an upsert request would need to leave it unchanged.
 */
@Component
class BusinessPartnerInputContentMapper {

    /**
     * Returns the content of [entity].
     */
    fun toContent(entity: BusinessPartnerDb): BusinessPartnerInputContent =
        BusinessPartnerInputContent(
            nameParts = entity.nameParts.toList(),
            identifiers = entity.identifiers.mapTo(linkedSetOf()) { BusinessPartnerInputContent.Identifier(it.type, it.value, it.issuingBody) },
            states = entity.states.mapTo(linkedSetOf(), ::toContent),
            roles = entity.roles.toCollection(linkedSetOf()),
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = BusinessPartnerInputContent.LegalEntity(
                bpn = entity.bpnL,
                legalName = entity.legalName,
                shortName = entity.shortName,
                legalForm = entity.legalForm,
                ownershipUltimate = entity.ownershipUltimate,
                ultimateOwnerBpnl = entity.ultimateOwnerBpnl
            ),
            site = BusinessPartnerInputContent.Site(bpn = entity.bpnS, name = entity.siteName),
            address = BusinessPartnerInputContent.Address(
                bpn = entity.bpnA,
                name = entity.addressName,
                addressType = entity.postalAddress.addressType,
                physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(::toValue),
                alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(::toValue)
            ),
            scriptVariants = entity.scriptVariants.mapTo(linkedSetOf(), ::toValue),
            additionalSites = entity.additionalSites.mapTo(linkedSetOf()) { BusinessPartnerInputValues.AdditionalSite(bpn = it.bpn, name = it.name) }
        )

    private fun toContent(state: StateDb) =
        BusinessPartnerInputContent.State(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerType = state.businessPartnerTyp)

    private fun toValue(address: PhysicalPostalAddressDb) =
        BusinessPartnerInputValues.PhysicalPostalAddress(
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

    private fun toValue(address: AlternativePostalAddressDb) =
        BusinessPartnerInputValues.AlternativePostalAddress(
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

    private fun toValue(scriptVariant: BusinessPartnerScriptVariantDb) =
        BusinessPartnerInputValues.ScriptVariant(
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
        BusinessPartnerInputValues.PhysicalAddressScriptVariant(
            city = address?.city,
            district = address?.district,
            street = toValue(address?.street),
            industrialZone = address?.industrialZone,
            building = address?.building,
            floor = address?.floor,
            door = address?.door
        )

    private fun toValue(street: StreetScriptVariantDb?) =
        BusinessPartnerInputValues.StreetScriptVariant(
            name = street?.name,
            direction = street?.direction,
            namePrefix = street?.namePrefix,
            additionalNamePrefix = street?.additionalNamePrefix,
            nameSuffix = street?.nameSuffix,
            additionalNameSuffix = street?.additionalNameSuffix
        )
}
