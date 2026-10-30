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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.springframework.stereotype.Component
import kotlin.reflect.KProperty

/**
 * Maps a stored business partner output onto the output data that rewrites it unchanged.
 *
 * A stored output always carries the BPNs, address and confidence criteria of what it describes, so a missing one
 * fails the mapping as an internal error.
 */
@Component
class OutputUpsertDataMapper(
    private val valueMapper: BusinessPartnerValueMapper
) {

    /**
     * Returns the output data [output] holds.
     */
    fun toUpsertData(output: BusinessPartnerDb): BusinessPartnerOutputContentParsed =
        with(output) {
            BusinessPartnerOutputContentParsed(
                nameParts = nameParts,
                identifiers = identifiers.map { IdentifierRaw(it.type, it.value, it.issuingBody, it.businessPartnerType) },
                states = states.map { StateRaw(it.validFrom, it.validTo, it.type, it.businessPartnerTyp) },
                roles = roles.toList(),
                isOwnCompanyData = isOwnCompanyData,
                legalEntityBpn = bpnL ?: throw missing(BusinessPartnerDb::bpnL, id),
                siteBpn = bpnS,
                addressBpn = bpnA ?: throw missing(BusinessPartnerDb::bpnA, id),
                legalName = legalName,
                legalForm = legalForm,
                shortName = shortName,
                siteName = siteName,
                addressName = addressName,
                addressType = postalAddress.addressType ?: throw missing(PostalAddressDb::addressType, id),
                physicalPostalAddress = toPhysicalPostalAddress(postalAddress.physicalPostalAddress ?: throw missing(PostalAddressDb::physicalPostalAddress, id)),
                alternativePostalAddress = postalAddress.alternativePostalAddress?.let(::toAlternativeAddress),
                legalEntityConfidence = toConfidence(legalEntityConfidence ?: throw missing(BusinessPartnerDb::legalEntityConfidence, id)),
                siteConfidence = siteConfidence?.let(::toConfidence),
                addressConfidence = toConfidence(addressConfidence ?: throw missing(BusinessPartnerDb::addressConfidence, id)),
                legalEntityUpdatedAt = legalEntityUpdatedAt,
                siteUpdatedAt = siteUpdatedAt,
                addressUpdatedAt = addressUpdatedAt,
                scriptVariants = scriptVariants.map(valueMapper::toValue),
                legalEntityGoldenRecordRelations = legalEntityGoldenRecordRelations.map { GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn) },
                siteGoldenRecordRelations = siteGoldenRecordRelations.map { GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn) },
                addressGoldenRecordRelations = addressGoldenRecordRelations.map { GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn) },
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl,
                additionalSites = additionalSites.map { AdditionalSiteRaw(it.bpn ?: throw missing(AdditionalSiteDb::bpn, id), it.name) }
            )
        }

    private fun toPhysicalPostalAddress(entity: PhysicalPostalAddressDb) =
        PhysicalPostalAddressRaw(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinate),
            country = entity.country ?: throw missing(PhysicalPostalAddressDb::country),
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            administrativeAreaLevel2 = entity.administrativeAreaLevel2,
            administrativeAreaLevel3 = entity.administrativeAreaLevel3,
            postalCode = entity.postalCode,
            city = entity.city ?: throw missing(PhysicalPostalAddressDb::city),
            district = entity.district,
            street = entity.street?.let(::toStreet),
            companyPostalCode = entity.companyPostalCode,
            industrialZone = entity.industrialZone,
            building = entity.building,
            floor = entity.floor,
            door = entity.door,
            taxJurisdictionCode = entity.taxJurisdictionCode
        )

    private fun toAlternativeAddress(entity: AlternativePostalAddressDb) =
        AlternativeAddressRaw(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinate),
            country = entity.country ?: throw missing(AlternativePostalAddressDb::country),
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            postalCode = entity.postalCode,
            city = entity.city ?: throw missing(AlternativePostalAddressDb::city),
            deliveryServiceType = entity.deliveryServiceType ?: throw missing(AlternativePostalAddressDb::deliveryServiceType),
            deliveryServiceQualifier = entity.deliveryServiceQualifier,
            deliveryServiceNumber = entity.deliveryServiceNumber ?: throw missing(AlternativePostalAddressDb::deliveryServiceNumber)
        )

    private fun toStreet(entity: StreetDb) =
        StreetRaw(
            namePrefix = entity.namePrefix,
            additionalNamePrefix = entity.additionalNamePrefix,
            name = entity.name,
            nameSuffix = entity.nameSuffix,
            additionalNameSuffix = entity.additionalNameSuffix,
            houseNumber = entity.houseNumber,
            houseNumberSupplement = entity.houseNumberSupplement,
            milestone = entity.milestone,
            direction = entity.direction
        )

    private fun toConfidence(entity: ConfidenceCriteriaDb) =
        ConfidenceCriteriaRaw(
            sharedByOwner = entity.sharedByOwner,
            checkedByExternalDataSource = entity.checkedByExternalDataSource,
            numberOfSharingMembers = entity.numberOfBusinessPartners,
            lastConfidenceCheckAt = entity.lastConfidenceCheckAt,
            nextConfidenceCheckAt = entity.nextConfidenceCheckAt,
            confidenceLevel = entity.confidenceLevel
        )

    private fun toGeoCoordinate(entity: GeographicCoordinateDb) =
        GeoCoordinateRaw(longitude = entity.longitude, latitude = entity.latitude, altitude = entity.altitude)

    private fun missing(property: KProperty<*>, entityId: Long? = null) =
        BpdmNullMappingException(BusinessPartnerDb::class, BusinessPartnerOutputContentParsed::class, property, entityId.toString())
}
