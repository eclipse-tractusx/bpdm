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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StreetRaw
import org.springframework.stereotype.Component

/**
 * Maps the business partner input request of the API to the unparsed upsert model, without judging its content.
 */
@Component
class BusinessPartnerInputRequestMapper {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertIn(request: BusinessPartnerInputRequest): BusinessPartnerInputUpsertRaw =
        BusinessPartnerInputUpsertRaw(
            externalId = request.externalId,
            nameParts = request.nameParts,
            identifiers = request.identifiers.map { BusinessPartnerInputUpsertRaw.Identifier(it.type, it.value, it.issuingBody) },
            states = toStates(request.states),
            roles = request.roles.toList(),
            isOwnCompanyData = request.isOwnCompanyData,
            legalEntity = with(request.legalEntity) {
                BusinessPartnerInputUpsertRaw.LegalEntity(
                    bpn = legalEntityBpn,
                    legalName = legalName,
                    shortName = shortName,
                    legalForm = legalForm,
                    ownershipUltimate = ownershipUltimate,
                    ultimateOwnerBpnl = ultimateOwnerBpnl,
                    states = toStates(states)
                )
            },
            site = BusinessPartnerInputUpsertRaw.Site(bpn = request.site.siteBpn, name = request.site.name, states = toStates(request.site.states)),
            address = with(request.address) {
                BusinessPartnerInputUpsertRaw.Address(
                    bpn = addressBpn,
                    name = name,
                    addressType = addressType,
                    physicalPostalAddress = toValue(physicalPostalAddress),
                    alternativePostalAddress = alternativePostalAddress?.let(::toValue),
                    states = toStates(states)
                )
            },
            externalSequenceTimestamp = request.externalSequenceTimestamp,
            scriptVariants = request.scriptVariants.map(::toValue),
            additionalSites = request.additionalSites.map { BusinessPartnerValuesRaw.AdditionalSite(bpn = it.siteBpn, name = it.name) }
        )

    private fun toStates(states: Collection<BusinessPartnerStateDto>) =
        states.map { BusinessPartnerInputUpsertRaw.State(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    private fun toValue(address: PhysicalPostalAddressDto) =
        with(address) {
            BusinessPartnerValuesRaw.PhysicalPostalAddress(
                geographicCoordinates = geographicCoordinates?.let(::toValue),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                administrativeAreaLevel2 = administrativeAreaLevel2,
                administrativeAreaLevel3 = administrativeAreaLevel3,
                postalCode = postalCode,
                city = city,
                district = district,
                street = street?.let(::toValue),
                companyPostalCode = companyPostalCode,
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door,
                taxJurisdictionCode = taxJurisdictionCode
            )
        }

    private fun toValue(address: AlternativePostalAddressDto) =
        with(address) {
            BusinessPartnerValuesRaw.AlternativePostalAddress(
                geographicCoordinates = geographicCoordinates?.let(::toValue),
                country = country,
                administrativeAreaLevel1 = administrativeAreaLevel1,
                postalCode = postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun toValue(street: StreetDto) =
        with(street) {
            StreetRaw(
                namePrefix = namePrefix,
                additionalNamePrefix = additionalNamePrefix,
                name = name,
                nameSuffix = nameSuffix,
                additionalNameSuffix = additionalNameSuffix,
                houseNumber = houseNumber,
                houseNumberSupplement = houseNumberSupplement,
                milestone = milestone,
                direction = direction
            )
        }

    private fun toValue(coordinate: GeoCoordinateDto) =
        GeoCoordinateRaw(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    private fun toValue(scriptVariant: BusinessPartnerScriptVariantDto) =
        with(scriptVariant) {
            BusinessPartnerValuesRaw.ScriptVariant(
                scriptCode = scriptCode,
                nameParts = nameParts,
                legalName = legalEntity.legalName,
                shortName = legalEntity.shortName,
                siteName = site.name,
                addressName = address.name,
                physicalAddress = with(address.physicalAddress) {
                    BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
                        city = city,
                        district = district,
                        street = with(street) {
                            BusinessPartnerValuesRaw.StreetScriptVariant(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix)
                        },
                        industrialZone = industrialZone,
                        building = building,
                        floor = floor,
                        door = door
                    )
                },
                alternativeAddressCity = address.alternativeAddress?.city
            )
        }
}
