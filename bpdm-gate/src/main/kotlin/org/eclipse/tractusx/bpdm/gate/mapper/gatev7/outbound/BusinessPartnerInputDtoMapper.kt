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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputContent
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputRecord
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputValues
import org.eclipse.tractusx.bpdm.gate.model.value.GeoCoordinate
import org.eclipse.tractusx.bpdm.gate.model.value.Street
import org.springframework.stereotype.Component

/**
 * Maps a business partner input record to the input response of the API.
 */
@Component
class BusinessPartnerInputDtoMapper {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerInputRecord): BusinessPartnerInputDto {
        val content = record.content
        return BusinessPartnerInputDto(
            externalId = record.externalId,
            nameParts = content.nameParts,
            identifiers = content.identifiers.map { BusinessPartnerIdentifierDto(type = it.type, value = it.value, issuingBody = it.issuingBody) },
            states = toStateDtos(content.states, BusinessPartnerType.GENERIC),
            roles = content.roles,
            isOwnCompanyData = content.isOwnCompanyData,
            legalEntity = with(content.legalEntity) {
                LegalEntityRepresentationInputDto(
                    legalEntityBpn = bpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    ownershipUltimate = ownershipUltimate,
                    ultimateOwnerBpnl = ultimateOwnerBpnl,
                    states = toStateDtos(content.states, BusinessPartnerType.LEGAL_ENTITY)
                )
            },
            site = SiteRepresentationInputDto(
                siteBpn = content.site.bpn,
                name = content.site.name,
                states = toStateDtos(content.states, BusinessPartnerType.SITE)
            ),
            address = with(content.address) {
                AddressRepresentationInputDto(
                    addressBpn = bpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = physicalPostalAddress?.let(::toDto) ?: PhysicalPostalAddressDto(),
                    alternativePostalAddress = alternativePostalAddress?.let(::toDto),
                    states = toStateDtos(content.states, BusinessPartnerType.ADDRESS)
                )
            },
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            scriptVariants = content.scriptVariants.map(::toDto),
            additionalSites = content.additionalSites.map { AdditionalSiteInputDto(siteBpn = it.bpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }

    private fun toStateDtos(states: Collection<BusinessPartnerInputContent.State>, businessPartnerType: BusinessPartnerType) =
        states.filter { it.businessPartnerType == businessPartnerType }
            .map { BusinessPartnerStateDto(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    private fun toDto(address: BusinessPartnerInputValues.PhysicalPostalAddress) =
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

    private fun toDto(address: BusinessPartnerInputValues.AlternativePostalAddress) =
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

    private fun toDto(street: Street) =
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

    private fun toDto(coordinate: GeoCoordinate) =
        GeoCoordinateDto(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    private fun toDto(scriptVariant: BusinessPartnerInputValues.ScriptVariant) =
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
