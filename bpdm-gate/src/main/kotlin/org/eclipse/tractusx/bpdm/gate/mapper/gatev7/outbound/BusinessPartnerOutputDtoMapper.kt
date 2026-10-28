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

import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.response.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SiteGoldenRecordRelationType
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Maps a business partner output record to the output response of the API.
 */
@Component
class BusinessPartnerOutputDtoMapper(
    private val valueDtoMapper: BusinessPartnerValueDtoMapper
) {

    /**
     * Returns the API response for [record].
     */
    fun toDto(record: BusinessPartnerOutputRecord): BusinessPartnerOutputDto =
        BusinessPartnerOutputDto(
            externalId = record.externalId,
            nameParts = record.nameParts,
            identifiers = record.identifiers.map(valueDtoMapper::toDto),
            states = record.states.map(valueDtoMapper::toDto),
            roles = record.roles,
            isOwnCompanyData = record.isOwnCompanyData,
            legalEntity = toDto(record.legalEntity),
            site = record.site?.let(::toDto),
            address = toDto(record.address),
            externalSequenceTimestamp = record.externalSequenceTimestamp,
            scriptVariants = record.scriptVariants.map(valueDtoMapper::toDto),
            additionalSites = record.additionalSites.map { AdditionalSiteOutputDto(siteBpn = it.siteBpn, name = it.name) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toDto(legalEntity: BusinessPartnerOutputRecord.LegalEntity) =
        LegalEntityRepresentationOutputDto(
            legalEntityBpn = legalEntity.bpn,
            legalName = legalEntity.legalName,
            shortName = legalEntity.shortName,
            legalForm = legalEntity.legalForm,
            confidenceCriteria = valueDtoMapper.toDto(legalEntity.confidence),
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            states = legalEntity.states.map(valueDtoMapper::toDto),
            goldenRecordRelations = legalEntity.goldenRecordRelations.map { LegalEntityGoldenRecordRelationDto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = legalEntity.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(site: BusinessPartnerOutputRecord.Site) =
        SiteRepresentationOutputDto(
            siteBpn = site.bpn,
            name = site.name,
            confidenceCriteria = valueDtoMapper.toDto(site.confidence),
            states = site.states.map(valueDtoMapper::toDto),
            goldenRecordRelations = site.goldenRecordRelations.map { SiteGoldenRecordRelationDto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = site.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(address: BusinessPartnerOutputRecord.Address) =
        AddressComponentOutputDto(
            addressBpn = address.bpn,
            name = address.name,
            addressType = address.addressType,
            physicalPostalAddress = address.physicalPostalAddress?.let(valueDtoMapper::toDto) ?: PhysicalPostalAddressDto(),
            alternativePostalAddress = address.alternativePostalAddress?.let(valueDtoMapper::toDto) ?: AlternativePostalAddressDto(),
            confidenceCriteria = valueDtoMapper.toDto(address.confidence),
            states = address.states.map(valueDtoMapper::toDto),
            goldenRecordRelations = address.goldenRecordRelations.map { AddressGoldenRecordRelationDto(toDto(it.type), it.sourceBpn, it.targetBpn) },
            updatedAt = address.updatedAt ?: Instant.EPOCH
        )

    private fun toDto(type: LegalEntityGoldenRecordRelationType) =
        when (type) {
            LegalEntityGoldenRecordRelationType.IsAlternativeHeadquarterFor -> LegalEntityGoldenRecordRelationTypeDto.IsAlternativeHeadquarterFor
            LegalEntityGoldenRecordRelationType.IsManagedBy -> LegalEntityGoldenRecordRelationTypeDto.IsManagedBy
            LegalEntityGoldenRecordRelationType.IsOwnedBy -> LegalEntityGoldenRecordRelationTypeDto.IsOwnedBy
            LegalEntityGoldenRecordRelationType.IsReplacedBy -> LegalEntityGoldenRecordRelationTypeDto.IsReplacedBy
        }

    private fun toDto(type: SiteGoldenRecordRelationType) =
        when (type) {
            SiteGoldenRecordRelationType.IsReplacedBy -> SiteGoldenRecordRelationTypeDto.IsReplacedBy
        }

    private fun toDto(type: AddressGoldenRecordRelationType) =
        when (type) {
            AddressGoldenRecordRelationType.IsReplacedBy -> AddressGoldenRecordRelationTypeDto.IsReplacedBy
        }
}
