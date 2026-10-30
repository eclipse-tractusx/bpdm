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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.springframework.stereotype.Component

/**
 * Maps the value parts of a business partner to their API form, the same in every stage and API version.
 */
@Component
class BusinessPartnerValueDtoMapper {

    /**
     * Returns the API form of an identifier.
     */
    fun toDto(identifier: IdentifierRaw) =
        BusinessPartnerIdentifierDto(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody)

    /**
     * Returns the API form of a business state.
     */
    fun toDto(state: StateRaw) =
        BusinessPartnerStateDto(type = state.type, validFrom = state.validFrom, validTo = state.validTo)

    /**
     * Returns the API form of confidence criteria.
     */
    fun toDto(confidence: ConfidenceCriteriaRaw) =
        ConfidenceCriteriaDto(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfSharingMembers,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt,
            confidenceLevel = confidence.confidenceLevel
        )

    /**
     * Returns the API form of a physical postal address.
     */
    fun toDto(address: BusinessPartnerValuesRaw.PhysicalPostalAddress) =
        PhysicalPostalAddressDto(
            geographicCoordinates = address.geographicCoordinates?.let(::toDto),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            administrativeAreaLevel2 = address.administrativeAreaLevel2,
            administrativeAreaLevel3 = address.administrativeAreaLevel3,
            postalCode = address.postalCode,
            city = address.city,
            district = address.district,
            street = address.street?.let(::toDto),
            companyPostalCode = address.companyPostalCode,
            industrialZone = address.industrialZone,
            building = address.building,
            floor = address.floor,
            door = address.door,
            taxJurisdictionCode = address.taxJurisdictionCode
        )

    /**
     * Returns the API form of an alternative postal address.
     */
    fun toDto(address: BusinessPartnerValuesRaw.AlternativePostalAddress) =
        AlternativePostalAddressDto(
            geographicCoordinates = address.geographicCoordinates?.let(::toDto),
            country = address.country,
            administrativeAreaLevel1 = address.administrativeAreaLevel1,
            postalCode = address.postalCode,
            city = address.city,
            deliveryServiceType = address.deliveryServiceType,
            deliveryServiceQualifier = address.deliveryServiceQualifier,
            deliveryServiceNumber = address.deliveryServiceNumber
        )

    private fun toDto(street: StreetRaw) =
        StreetDto(
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

    private fun toDto(coordinate: GeoCoordinateRaw) =
        GeoCoordinateDto(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    /**
     * Returns the API form of a script variant.
     */
    fun toDto(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        BusinessPartnerScriptVariantDto(
            scriptCode = scriptVariant.scriptCode,
            nameParts = scriptVariant.nameParts,
            legalEntity = LegalEntityScriptVariantDto(legalName = scriptVariant.legalName, shortName = scriptVariant.shortName),
            site = SiteScriptVariantDto(name = scriptVariant.siteName),
            address = AddressScriptVariantDto(
                name = scriptVariant.addressName,
                physicalAddress = with(scriptVariant.physicalAddress) {
                    PhysicalAddressScriptVariantDto(
                        city = city,
                        district = district,
                        street = with(street) {
                            StreetScriptVariantDto(
                                name = name,
                                direction = direction,
                                namePrefix = namePrefix,
                                additionalNamePrefix = additionalNamePrefix,
                                nameSuffix = nameSuffix,
                                additionalNameSuffix = additionalNameSuffix
                            )
                        },
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                },
                alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariantDto(city = it) }
            )
        )
}
