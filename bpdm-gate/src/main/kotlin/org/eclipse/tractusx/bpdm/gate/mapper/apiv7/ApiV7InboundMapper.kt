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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv7

import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.*
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.output.SharableRelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.sharingstate.RelationSharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.springframework.stereotype.Component

/**
 * Maps API v7 requests to their raw model.
 */
@Component
class ApiV7InboundMapper {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertIn(request: BusinessPartnerInputV7Request): BusinessPartnerInputUpsertRaw =
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

    private fun toStates(states: Collection<BusinessPartnerStateV7Dto>) =
        states.map { BusinessPartnerInputUpsertRaw.State(type = it.type, validFrom = it.validFrom, validTo = it.validTo) }

    private fun toValue(address: PhysicalPostalAddressV7Dto) =
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

    private fun toValue(address: AlternativePostalAddressV7Dto) =
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

    private fun toValue(street: StreetV7Dto) =
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

    private fun toValue(scriptVariant: BusinessPartnerScriptVariantV7Dto) =
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
    fun toUpsertRequest(entry: RelationPutV7Entry): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = entry.externalId,
            relationType = toModel(entry.relationType),
            sourceExternalId = entry.businessPartnerSourceExternalId,
            targetExternalId = entry.businessPartnerTargetExternalId,
            validityPeriods = entry.validityPeriods.map { RelationValidityPeriodRaw(it.validFrom, it.validTo) },
            reasonCode = entry.reasonCode
        )

    /**
     * Returns the Gate's sharing state type for [sharingStateType].
     */
    fun toModel(sharingStateType: SharingStateTypeV7): SharingStateType =
        when (sharingStateType) {
            SharingStateTypeV7.Pending -> SharingStateType.Pending
            SharingStateTypeV7.Success -> SharingStateType.Success
            SharingStateTypeV7.Error -> SharingStateType.Error
            SharingStateTypeV7.Initial -> SharingStateType.Initial
            SharingStateTypeV7.Ready -> SharingStateType.Ready
        }

    /**
     * Returns the Gate's relation input type for [relationType].
     */
    fun toModel(relationType: RelationTypeV7): RelationType =
        when (relationType) {
            RelationTypeV7.IsManagedBy -> RelationType.IsManagedBy
            RelationTypeV7.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            RelationTypeV7.IsOwnedBy -> RelationType.IsOwnedBy
            RelationTypeV7.IsReplacedBy -> RelationType.IsReplacedBy
        }

    /**
     * Returns the Gate's relation output type for [relationType].
     */
    fun toModel(relationType: SharableRelationTypeV7): SharableRelationType =
        when (relationType) {
            SharableRelationTypeV7.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
            SharableRelationTypeV7.IsManagedBy -> SharableRelationType.IsManagedBy
            SharableRelationTypeV7.IsOwnedBy -> SharableRelationType.IsOwnedBy
            SharableRelationTypeV7.IsReplacedBy -> SharableRelationType.IsReplacedBy
        }

    /**
     * Returns the Gate's relation sharing state type for [sharingStateType].
     */
    fun toModel(sharingStateType: RelationSharingStateTypeV7): RelationSharingStateType =
        when (sharingStateType) {
            RelationSharingStateTypeV7.Ready -> RelationSharingStateType.Ready
            RelationSharingStateTypeV7.Pending -> RelationSharingStateType.Pending
            RelationSharingStateTypeV7.Error -> RelationSharingStateType.Error
            RelationSharingStateTypeV7.Success -> RelationSharingStateType.Success
        }
}
