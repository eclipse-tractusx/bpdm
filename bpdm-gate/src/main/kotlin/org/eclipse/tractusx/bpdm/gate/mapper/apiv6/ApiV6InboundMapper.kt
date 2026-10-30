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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.AlternativePostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.BusinessPartnerScriptVariantV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.BusinessPartnerStateV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.PhysicalPostalAddressV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.StreetV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.BusinessPartnerInputV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutV6Entry
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Component

/**
 * Maps API v6 requests to their raw model.
 */
@Component
class ApiV6InboundMapper {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertIn(request: BusinessPartnerInputV6Request): BusinessPartnerInputUpsertRaw =
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

    private fun toStates(states: Collection<BusinessPartnerStateV6Dto>) =
        states.map { BusinessPartnerInputUpsertRaw.State(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    private fun toValue(address: PhysicalPostalAddressV6Dto) =
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

    private fun toValue(address: AlternativePostalAddressV6Dto) =
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

    private fun toValue(street: StreetV6Dto) =
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

    private fun toValue(scriptVariant: BusinessPartnerScriptVariantV6Dto) =
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

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [entry].
     */
    fun toUpsertRequest(entry: RelationPutV6Entry): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = entry.externalId,
            relationType = toModel(entry.relationType),
            sourceExternalId = entry.businessPartnerSourceExternalId,
            targetExternalId = entry.businessPartnerTargetExternalId,
            validityPeriods = entry.validityPeriods.map { RelationValidityPeriodRaw(it.validFrom, it.validTo) },
            reasonCode = null
        )

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertRequest(request: RelationPostV6Request): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = request.externalId,
            relationType = toModel(request.relationType),
            sourceExternalId = request.businessPartnerSourceExternalId,
            targetExternalId = request.businessPartnerTargetExternalId,
            validityPeriods = emptyList(),
            reasonCode = null
        )

    /**
     * Returns the Gate's relation input type for [relationType].
     */
    fun toModel(relationType: RelationTypeV6): RelationType =
        when (relationType) {
            RelationTypeV6.IsManagedBy -> RelationType.IsManagedBy
            RelationTypeV6.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            RelationTypeV6.IsOwnedBy -> RelationType.IsOwnedBy
            RelationTypeV6.IsReplacedBy -> RelationType.IsReplacedBy
        }
}
