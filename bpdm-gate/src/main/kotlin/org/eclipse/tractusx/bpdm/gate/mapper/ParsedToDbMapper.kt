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
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.AdditionalSiteDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerInputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
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
     * Returns the output entity of [sharingState] holding [upsertData].
     */
    fun toEntity(upsertData: BusinessPartnerOutputContentParsed, sharingState: SharingStateDb): BusinessPartnerDb {
        val businessPartner =  with(upsertData) {
            BusinessPartnerDb(
                sharingState = sharingState,
                stage = StageType.Output,
                nameParts = nameParts.toMutableList(),
                roles = roles.toSortedSet(),
                identifiers = identifiers.map { it.toEntity() }.toSortedSet(),
                states = states.map { it.toEntity() }.toSortedSet(),
                shortName = shortName,
                legalName = legalName,
                siteName = siteName,
                addressName = addressName,
                legalForm = legalForm,
                isOwnCompanyData = isOwnCompanyData,
                bpnL = legalEntityBpn,
                bpnS = siteBpn,
                bpnA = addressBpn,
                ownershipUltimate = ownershipUltimate,
                ultimateOwnerBpnl = ultimateOwnerBpnl,
                postalAddress = PostalAddressDb(addressType, physicalPostalAddress.toEntity(), alternativePostalAddress?.toEntity()),
                legalEntityConfidence = legalEntityConfidence.toEntity(),
                siteConfidence = siteConfidence?.toEntity(),
                addressConfidence = addressConfidence.toEntity(),
                legalEntityUpdatedAt = legalEntityUpdatedAt,
                siteUpdatedAt = siteUpdatedAt,
                addressUpdatedAt = addressUpdatedAt
            )
        }

        upsertData.scriptVariants.map { toScriptVariant(businessPartner, it) }
            .forEach { businessPartner.scriptVariants.add(it) }

        upsertData.legalEntityGoldenRecordRelations.map { toLegalEntityGoldenRecordRelation(it) }
            .forEach { businessPartner.legalEntityGoldenRecordRelations.add(it) }
        upsertData.siteGoldenRecordRelations.map { toSiteGoldenRecordRelation(it) }
            .forEach { businessPartner.siteGoldenRecordRelations.add(it) }
        upsertData.addressGoldenRecordRelations.map { toAddressGoldenRecordRelation(it) }
            .forEach { businessPartner.addressGoldenRecordRelations.add(it) }
        upsertData.additionalSites.map { AdditionalSiteDb(it.siteBpn, it.name) }
            .forEach { businessPartner.additionalSites.add(it) }

        return businessPartner
    }

    private fun toScriptVariant(businessPartner: BusinessPartnerDb, scriptVariant: BusinessPartnerValuesRaw.ScriptVariant) =
        BusinessPartnerScriptVariantDb(
            scriptCode = scriptVariant.scriptCode,
            businessPartner = businessPartner,
            nameParts = scriptVariant.nameParts.toMutableList(),
            shortName = scriptVariant.shortName,
            legalName = scriptVariant.legalName,
            siteName = scriptVariant.siteName,
            addressName = scriptVariant.addressName,
            physicalAddress = toEntity(scriptVariant.physicalAddress),
            alternativeAddress = scriptVariant.alternativeAddressCity?.let(::toAlternativeAddressScriptVariant)
        )

    private fun toLegalEntityGoldenRecordRelation(relation: GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>) =
        LegalEntityGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toSiteGoldenRecordRelation(relation: GoldenRecordRelationRaw<SiteGoldenRecordRelationType>) =
        SiteGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun toAddressGoldenRecordRelation(relation: GoldenRecordRelationRaw<AddressGoldenRecordRelationType>) =
        AddressGoldenRecordRelationDb(
            relationType = relation.type,
            sourceBpn = relation.sourceBpn,
            targetBpn = relation.targetBpn
        )

    private fun IdentifierRaw.toEntity() =
        IdentifierDb(type = type, value = value, issuingBody = issuingBody, businessPartnerType = businessPartnerType)

    private fun StateRaw.toEntity() =
        StateDb(type = type, validFrom = validFrom, validTo = validTo, businessPartnerTyp = businessPartnerType)

    private fun ConfidenceCriteriaRaw.toEntity() =
        ConfidenceCriteriaDb(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers, lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)


    private fun PhysicalPostalAddressRaw.toEntity() =
        PhysicalPostalAddressDb(
            geographicCoordinates = geographicCoordinates?.let(::toEntity),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            postalCode = postalCode,
            city = city,
            district = district,
            street = street?.let(::toEntity),
            companyPostalCode = companyPostalCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
            taxJurisdictionCode = taxJurisdictionCode
        )

    private fun AlternativeAddressRaw.toEntity() =
        AlternativePostalAddressDb(
            geographicCoordinates = geographicCoordinates?.let(::toEntity),
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            postalCode = postalCode,
            city = city,
            deliveryServiceType = deliveryServiceType,
            deliveryServiceQualifier = deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber
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
