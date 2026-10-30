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

package org.eclipse.tractusx.bpdm.gate.mapper

import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
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
 * Maps persisted entities to the parsed models they guarantee.
 */
@Component
class DbToParsedMapper {

    /**
     * Returns the content of [entity].
     */
    fun toContent(entity: BusinessPartnerDb): BusinessPartnerInputContentParsed =
        BusinessPartnerInputContentParsed(
            nameParts = entity.nameParts.toList(),
            identifiers = entity.identifiers.mapTo(linkedSetOf()) { BusinessPartnerInputContentParsed.Identifier(it.type, it.value, it.issuingBody) },
            states = entity.states.mapTo(linkedSetOf(), ::toContent),
            roles = entity.roles.toCollection(linkedSetOf()),
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = BusinessPartnerInputContentParsed.LegalEntity(
                bpn = entity.bpnL,
                legalName = entity.legalName,
                shortName = entity.shortName,
                legalForm = entity.legalForm,
                ownershipUltimate = entity.ownershipUltimate,
                ultimateOwnerBpnl = entity.ultimateOwnerBpnl
            ),
            site = BusinessPartnerInputContentParsed.Site(bpn = entity.bpnS, name = entity.siteName),
            address = BusinessPartnerInputContentParsed.Address(
                bpn = entity.bpnA,
                name = entity.addressName,
                addressType = entity.postalAddress.addressType,
                physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(::toValue),
                alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(::toValue)
            ),
            scriptVariants = entity.scriptVariants.mapTo(linkedSetOf(), ::toValue),
            additionalSites = entity.additionalSites.mapTo(linkedSetOf()) { BusinessPartnerValuesRaw.AdditionalSite(bpn = it.bpn, name = it.name) }
        )

    private fun toContent(state: StateDb) =
        BusinessPartnerInputContentParsed.State(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerType = state.businessPartnerTyp)

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
                scriptVariants = scriptVariants.map(::toValue),
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
            geographicCoordinates = entity.geographicCoordinates?.let(::toValue),
            country = entity.country ?: throw missing(PhysicalPostalAddressDb::country),
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            administrativeAreaLevel2 = entity.administrativeAreaLevel2,
            administrativeAreaLevel3 = entity.administrativeAreaLevel3,
            postalCode = entity.postalCode,
            city = entity.city ?: throw missing(PhysicalPostalAddressDb::city),
            district = entity.district,
            street = entity.street?.let(::toValue),
            companyPostalCode = entity.companyPostalCode,
            industrialZone = entity.industrialZone,
            building = entity.building,
            floor = entity.floor,
            door = entity.door,
            taxJurisdictionCode = entity.taxJurisdictionCode
        )

    private fun toAlternativeAddress(entity: AlternativePostalAddressDb) =
        AlternativeAddressRaw(
            geographicCoordinates = entity.geographicCoordinates?.let(::toValue),
            country = entity.country ?: throw missing(AlternativePostalAddressDb::country),
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            postalCode = entity.postalCode,
            city = entity.city ?: throw missing(AlternativePostalAddressDb::city),
            deliveryServiceType = entity.deliveryServiceType ?: throw missing(AlternativePostalAddressDb::deliveryServiceType),
            deliveryServiceQualifier = entity.deliveryServiceQualifier,
            deliveryServiceNumber = entity.deliveryServiceNumber ?: throw missing(AlternativePostalAddressDb::deliveryServiceNumber)
        )

    /**
     * Returns the confidence criteria stored in [entity].
     */
    fun toConfidence(entity: ConfidenceCriteriaDb) =
        ConfidenceCriteriaRaw(
            sharedByOwner = entity.sharedByOwner,
            checkedByExternalDataSource = entity.checkedByExternalDataSource,
            numberOfSharingMembers = entity.numberOfBusinessPartners,
            lastConfidenceCheckAt = entity.lastConfidenceCheckAt,
            nextConfidenceCheckAt = entity.nextConfidenceCheckAt,
            confidenceLevel = entity.confidenceLevel
        )

    private fun missing(property: KProperty<*>, entityId: Long? = null) =
        BpdmNullMappingException(BusinessPartnerDb::class, BusinessPartnerOutputContentParsed::class, property, entityId.toString())

    /**
     * Returns the value of a stored physical postal address.
     */
    fun toValue(address: PhysicalPostalAddressDb) =
        BusinessPartnerValuesRaw.PhysicalPostalAddress(
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
        BusinessPartnerValuesRaw.AlternativePostalAddress(
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
        StreetRaw(
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
        GeoCoordinateRaw(longitude = coordinate.longitude, latitude = coordinate.latitude, altitude = coordinate.altitude)

    /**
     * Returns the value of a stored script variant.
     */
    fun toValue(scriptVariant: BusinessPartnerScriptVariantDb) =
        BusinessPartnerValuesRaw.ScriptVariant(
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
        BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
            city = address?.city,
            district = address?.district,
            street = toValue(address?.street),
            industrialZone = address?.industrialZone,
            building = address?.building,
            floor = address?.floor,
            door = address?.door
        )

    private fun toValue(street: StreetScriptVariantDb?) =
        BusinessPartnerValuesRaw.StreetScriptVariant(
            name = street?.name,
            direction = street?.direction,
            namePrefix = street?.namePrefix,
            additionalNamePrefix = street?.additionalNamePrefix,
            nameSuffix = street?.nameSuffix,
            additionalNameSuffix = street?.additionalNameSuffix
        )
}
