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

package org.eclipse.tractusx.bpdm.gate.mapper.pool.inbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseStateDto
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.value.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StreetRaw
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.AlternativePostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.PhysicalPostalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.RelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetDto
import org.eclipse.tractusx.bpdm.pool.api.model.StreetScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.springframework.stereotype.Component

/**
 * Maps the golden records the Pool returns onto the golden record requests the Gate aligns its output with.
 */
@Component
class PoolGoldenRecordRequestMapper {

    /**
     * Maps [dto] onto its legal entity golden record request.
     */
    fun toRequest(dto: LegalEntityWithLegalAddressVerboseDto): LegalEntityGoldenRecordRaw {
        val header = dto.header
        return LegalEntityGoldenRecordRaw(
            bpn = header.bpnl,
            legalName = header.legalName,
            shortName = header.legalShortName,
            legalForm = header.legalForm,
            identifiers = header.identifiers.map { IdentifierRaw(it.type, it.value, it.issuingBody, BusinessPartnerType.LEGAL_ENTITY) },
            states = header.states.map { toState(it, BusinessPartnerType.LEGAL_ENTITY) },
            confidence = toConfidence(header.confidenceCriteria),
            relations = header.relations.map(::toRelation),
            ownershipUltimate = header.ownershipUltimate,
            ultimateOwnerBpnl = header.ultimateOwnerBpnl,
            scriptVariants = dto.scriptVariants.map { LegalEntityGoldenRecordRaw.ScriptVariant(it.scriptCode, it.legalName, it.shortName) },
            updatedAt = header.updatedAt
        )
    }

    /**
     * Maps [dto] onto its site golden record request.
     */
    fun toRequest(dto: SiteVerboseDto): SiteGoldenRecordRaw =
        SiteGoldenRecordRaw(
            bpn = dto.bpns,
            name = dto.name,
            states = dto.states.map { toState(it, BusinessPartnerType.SITE) },
            confidence = toConfidence(dto.confidenceCriteria),
            relations = dto.relations.map(::toRelation),
            scriptVariants = dto.scriptVariants.map { SiteGoldenRecordRaw.ScriptVariant(it.scriptCode, it.name) },
            updatedAt = dto.updatedAt
        )

    /**
     * Maps [dto] onto its address golden record request.
     */
    fun toRequest(dto: LogisticAddressVerboseDto): AddressGoldenRecordRaw {
        val address = dto.address
        return AddressGoldenRecordRaw(
            bpn = address.bpna,
            name = address.name,
            addressType = address.addressType,
            identifiers = address.identifiers.map { IdentifierRaw(it.type, it.value, null, BusinessPartnerType.ADDRESS) },
            states = address.states.map { toState(it, BusinessPartnerType.ADDRESS) },
            physicalPostalAddress = toPhysicalPostalAddress(address.physicalPostalAddress),
            alternativePostalAddress = address.alternativePostalAddress?.let(::toAlternativeAddress),
            confidence = toConfidence(address.confidenceCriteria),
            relations = address.relations.map(::toRelation),
            siteBpn = address.bpnSite,
            additionalSiteBpns = address.additionalSites,
            scriptVariants = dto.scriptVariants.map(::toScriptVariant),
            updatedAt = address.updatedAt
        )
    }

    private fun toState(dto: IBaseStateDto, businessPartnerType: BusinessPartnerType) =
        StateRaw(validFrom = dto.validFrom, validTo = dto.validTo, type = dto.type!!, businessPartnerType = businessPartnerType)

    private fun toConfidence(dto: ConfidenceCriteriaDto) =
        ConfidenceCriteriaRaw(
            sharedByOwner = dto.sharedByOwner,
            checkedByExternalDataSource = dto.checkedByExternalDataSource,
            numberOfSharingMembers = dto.numberOfSharingMembers,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt,
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt,
            confidenceLevel = dto.confidenceLevel
        )

    private fun toRelation(dto: RelationVerboseDto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                LegalEntityRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
                LegalEntityRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
                LegalEntityRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationType.IsOwnedBy
                LegalEntityRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpnl,
            targetBpn = dto.businessPartnerTargetBpnl
        )

    private fun toRelation(dto: SiteRelationVerboseDto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                SiteRelationType.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpns,
            targetBpn = dto.businessPartnerTargetBpns
        )

    private fun toRelation(dto: AddressRelationVerboseDto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                AddressRelationType.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpna,
            targetBpn = dto.businessPartnerTargetBpna
        )

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddressVerboseDto) =
        PhysicalPostalAddressRaw(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeoCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city,
            district = dto.district,
            street = dto.street?.let(::toStreet),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door,
            taxJurisdictionCode = dto.taxJurisdictionCode
        )

    private fun toAlternativeAddress(dto: AlternativePostalAddressVerboseDto) =
        AlternativeAddressRaw(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeoCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber
        )

    private fun toGeoCoordinate(dto: GeoCoordinateDto) =
        GeoCoordinateRaw(longitude = dto.longitude, latitude = dto.latitude, altitude = dto.altitude)

    private fun toStreet(dto: StreetDto) =
        StreetRaw(
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            name = dto.name,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix,
            houseNumber = dto.houseNumber,
            houseNumberSupplement = dto.houseNumberSupplement,
            milestone = dto.milestone,
            direction = dto.direction
        )

    private fun toScriptVariant(dto: LogisticAddressScriptVariantDto) =
        AddressGoldenRecordRaw.ScriptVariant(
            scriptCode = dto.scriptCode,
            name = dto.address.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(dto.address.physicalAddress),
            alternativeAddressCity = dto.address.alternativeAddress?.city
        )

    private fun toPhysicalAddressScriptVariant(dto: PhysicalAddressScriptVariantDto) =
        BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
            city = dto.city,
            district = dto.district,
            street = toStreetScriptVariant(dto.street ?: StreetScriptVariantDto()),
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door
        )

    private fun toStreetScriptVariant(dto: StreetScriptVariantDto) =
        BusinessPartnerValuesRaw.StreetScriptVariant(
            name = dto.name,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )
}
