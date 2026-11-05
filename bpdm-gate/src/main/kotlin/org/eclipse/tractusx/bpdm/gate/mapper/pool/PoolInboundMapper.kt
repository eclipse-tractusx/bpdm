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

package org.eclipse.tractusx.bpdm.gate.mapper.pool

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseStateDto
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.GeoCoordinateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StreetRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AlternativePostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalAddressScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.PhysicalPostalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.StreetV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.springframework.stereotype.Component

/**
 * Maps Pool responses to their raw model.
 */
@Component
class PoolInboundMapper {

    /**
     * Maps [dto] onto its legal entity golden record request.
     */
    fun toRequest(dto: LegalEntityWithLegalAddressVerboseV7Dto): LegalEntityGoldenRecordRaw {
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
    fun toRequest(dto: SiteWithMainAddressVerboseV7Dto): SiteGoldenRecordRaw =
        SiteGoldenRecordRaw(
            bpn = dto.site.bpns,
            mainAddressBpn = dto.mainAddress.bpna,
            name = dto.site.name,
            states = dto.site.states.map { toState(it, BusinessPartnerType.SITE) },
            confidence = toConfidence(dto.site.confidenceCriteria),
            relations = dto.site.relations.map(::toRelation),
            scriptVariants = dto.site.scriptVariants.map { SiteGoldenRecordRaw.ScriptVariant(it.scriptCode, it.name) },
            updatedAt = dto.site.updatedAt
        )

    /**
     * Maps [dto] onto its address golden record request.
     */
    fun toRequest(dto: LogisticAddressVerboseV7Dto): AddressGoldenRecordRaw {
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
        StateRaw(
            validFrom = BusinessStateDates.receiveValidFrom(dto.validFrom),
            validTo = BusinessStateDates.receiveValidTo(dto.validTo),
            type = dto.type!!,
            businessPartnerType = businessPartnerType
        )

    private fun toConfidence(dto: ConfidenceCriteriaV7Dto) =
        ConfidenceCriteriaRaw(
            sharedByOwner = dto.sharedByOwner,
            checkedByExternalDataSource = dto.checkedByExternalDataSource,
            numberOfSharingMembers = dto.numberOfSharingMembers,
            lastConfidenceCheckAt = dto.lastConfidenceCheckAt,
            nextConfidenceCheckAt = dto.nextConfidenceCheckAt,
            confidenceLevel = dto.confidenceLevel
        )

    private fun toRelation(dto: RelationVerboseV7Dto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor
                LegalEntityRelationTypeV7.IsManagedBy -> LegalEntityGoldenRecordRelationType.IsManagedBy
                LegalEntityRelationTypeV7.IsOwnedBy -> LegalEntityGoldenRecordRelationType.IsOwnedBy
                LegalEntityRelationTypeV7.IsReplacedBy -> LegalEntityGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpnl,
            targetBpn = dto.businessPartnerTargetBpnl
        )

    private fun toRelation(dto: SiteRelationVerboseV7Dto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                SiteRelationTypeV7.IsReplacedBy -> SiteGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpns,
            targetBpn = dto.businessPartnerTargetBpns
        )

    private fun toRelation(dto: AddressRelationVerboseV7Dto) =
        GoldenRecordRelationRaw(
            type = when (dto.type) {
                AddressRelationTypeV7.IsReplacedBy -> AddressGoldenRecordRelationType.IsReplacedBy
            },
            sourceBpn = dto.businessPartnerSourceBpna,
            targetBpn = dto.businessPartnerTargetBpna
        )

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddressVerboseV7Dto) =
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

    private fun toAlternativeAddress(dto: AlternativePostalAddressVerboseV7Dto) =
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

    private fun toStreet(dto: StreetV7Dto) =
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

    private fun toScriptVariant(dto: LogisticAddressScriptVariantV7Dto) =
        AddressGoldenRecordRaw.ScriptVariant(
            scriptCode = dto.scriptCode,
            name = dto.address.addressName,
            physicalAddress = toPhysicalAddressScriptVariant(dto.address.physicalAddress),
            alternativeAddressCity = dto.address.alternativeAddress?.city
        )

    private fun toPhysicalAddressScriptVariant(dto: PhysicalAddressScriptVariantV7Dto) =
        BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
            city = dto.city,
            district = dto.district,
            street = toStreetScriptVariant(dto.street ?: StreetScriptVariantV7Dto()),
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door
        )

    private fun toStreetScriptVariant(dto: StreetScriptVariantV7Dto) =
        BusinessPartnerValuesRaw.StreetScriptVariant(
            name = dto.name,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )
}
