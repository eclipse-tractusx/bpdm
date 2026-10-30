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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.AdditionalSiteDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.springframework.stereotype.Component

/**
 * Maps parsed models to the entities they are persisted as.
 */
@Component
class ParsedToDbMapper {

    /**
     * Writes [scriptVariants] into the script variants of [businessPartner], matched by script code.
     */
    fun writeScriptVariants(scriptVariants: Collection<BusinessPartnerValuesRaw.ScriptVariant>, businessPartner: BusinessPartnerDb) {
        // Updated in place by script code: replacing the list would insert before the orphans are deleted and break the
        // unique (script_code, business_partner_id) constraint.
        val existingByCode = businessPartner.scriptVariants.associateBy { it.scriptCode }
        businessPartner.scriptVariants.removeIf { existing -> scriptVariants.none { it.scriptCode == existing.scriptCode } }

        scriptVariants.forEach { scriptVariant ->
            val target = existingByCode[scriptVariant.scriptCode]
                ?: BusinessPartnerScriptVariantDb(scriptVariant.scriptCode, businessPartner).also { businessPartner.scriptVariants.add(it) }
            writeScriptVariant(scriptVariant, target)
        }
    }

    private fun writeScriptVariant(scriptVariant: BusinessPartnerValuesRaw.ScriptVariant, target: BusinessPartnerScriptVariantDb) {
        target.nameParts.replace(scriptVariant.nameParts)
        target.legalName = scriptVariant.legalName
        target.shortName = scriptVariant.shortName
        target.siteName = scriptVariant.siteName
        target.addressName = scriptVariant.addressName
        target.physicalAddress = toEntity(scriptVariant.physicalAddress)
        target.alternativeAddress = scriptVariant.alternativeAddressCity?.let(::toAlternativeAddressScriptVariant)
    }

    /**
     * Returns the stored form of the legal entity golden record [relation].
     */
    fun toLegalEntityGoldenRecordRelation(relation: GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>) =
        LegalEntityGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    /**
     * Returns the stored form of the site golden record [relation].
     */
    fun toSiteGoldenRecordRelation(relation: GoldenRecordRelationRaw<SiteGoldenRecordRelationType>) =
        SiteGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    /**
     * Returns the stored form of the address golden record [relation].
     */
    fun toAddressGoldenRecordRelation(relation: GoldenRecordRelationRaw<AddressGoldenRecordRelationType>) =
        AddressGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    /**
     * Returns the stored form of the output [identifier].
     */
    fun toEntity(identifier: IdentifierRaw): IdentifierDb =
        IdentifierDb(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody, businessPartnerType = identifier.businessPartnerType)

    /**
     * Returns the stored form of the output [state].
     */
    fun toEntity(state: StateRaw): StateDb =
        StateDb(type = state.type, validFrom = state.validFrom, validTo = state.validTo, businessPartnerTyp = state.businessPartnerType)

    /**
     * Returns the stored form of [confidence].
     */
    fun toEntity(confidence: ConfidenceCriteriaRaw): ConfidenceCriteriaDb =
        with(confidence) {
            ConfidenceCriteriaDb(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)
        }

    /**
     * Returns the stored form of the output [additionalSite].
     */
    fun toEntity(additionalSite: AdditionalSiteRaw): AdditionalSiteDb =
        AdditionalSiteDb(additionalSite.siteBpn, additionalSite.name)

    /**
     * Returns the stored form of the output's physical postal [address].
     */
    fun toEntity(address: PhysicalPostalAddressRaw): PhysicalPostalAddressDb =
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
     * Returns the stored form of the output's alternative postal [address].
     */
    fun toEntity(address: AlternativeAddressRaw): AlternativePostalAddressDb =
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
