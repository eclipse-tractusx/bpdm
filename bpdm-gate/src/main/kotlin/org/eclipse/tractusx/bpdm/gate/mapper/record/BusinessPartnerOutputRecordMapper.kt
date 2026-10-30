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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.entity.generic.AdditionalSiteDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.BusinessPartnerOutputRecord
import org.springframework.stereotype.Component

/**
 * Maps a stored business partner output to its record.
 *
 * A stored output always carries the BPNs and confidence criteria of what it describes, so a missing one fails the
 * mapping as an internal error.
 */
@Component
class BusinessPartnerOutputRecordMapper(
    private val valueMapper: BusinessPartnerValueMapper
) {

    /**
     * Returns the record of [entity], showing [identifiers] and [states] as the business partner's own.
     */
    fun toRecord(entity: BusinessPartnerDb, identifiers: Collection<IdentifierDb>, states: Collection<StateDb>): BusinessPartnerOutputRecord {
        val externalId = entity.sharingState.externalId
        return BusinessPartnerOutputRecord(
            externalId = externalId,
            externalSequenceTimestamp = entity.externalSequenceTimestamp,
            nameParts = entity.nameParts.toList(),
            identifiers = identifiers.map(::toRecord),
            states = states.map(::toRecord),
            roles = entity.roles.toList(),
            isOwnCompanyData = entity.isOwnCompanyData,
            legalEntity = BusinessPartnerOutputRecord.LegalEntity(
                bpn = entity.bpnL ?: throw BpdmNullMappingException(BusinessPartnerDb::class, BusinessPartnerOutputRecord::class, BusinessPartnerDb::bpnL, externalId),
                legalName = entity.legalName,
                shortName = entity.shortName,
                legalForm = entity.legalForm,
                confidence = toRecord(entity.legalEntityConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing legal entity confidence criteria")),
                ownershipUltimate = entity.ownershipUltimate,
                ultimateOwnerBpnl = entity.ultimateOwnerBpnl,
                states = statesOf(entity, BusinessPartnerType.LEGAL_ENTITY),
                goldenRecordRelations = entity.legalEntityGoldenRecordRelations.map {
                    GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn)
                },
                updatedAt = entity.legalEntityUpdatedAt
            ),
            site = entity.bpnS?.let { siteBpn ->
                BusinessPartnerOutputRecord.Site(
                    bpn = siteBpn,
                    name = entity.siteName,
                    confidence = toRecord(entity.siteConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing site confidence criteria")),
                    states = statesOf(entity, BusinessPartnerType.SITE),
                    goldenRecordRelations = entity.siteGoldenRecordRelations.map {
                        GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn)
                    },
                    updatedAt = entity.siteUpdatedAt
                )
            },
            address = BusinessPartnerOutputRecord.Address(
                bpn = entity.bpnA ?: throw BpdmNullMappingException(BusinessPartnerDb::class, BusinessPartnerOutputRecord::class, BusinessPartnerDb::bpnA, externalId),
                name = entity.addressName,
                addressType = entity.postalAddress.addressType,
                physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(valueMapper::toValue),
                alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(valueMapper::toValue),
                confidence = toRecord(entity.addressConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing address confidence criteria")),
                states = statesOf(entity, BusinessPartnerType.ADDRESS),
                goldenRecordRelations = entity.addressGoldenRecordRelations.map {
                    GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn)
                },
                updatedAt = entity.addressUpdatedAt
            ),
            scriptVariants = entity.scriptVariants.map(valueMapper::toValue),
            additionalSites = entity.additionalSites.map {
                AdditionalSiteRaw(
                    siteBpn = it.bpn ?: throw BpdmNullMappingException(AdditionalSiteDb::class, BusinessPartnerOutputRecord::class, AdditionalSiteDb::bpn, externalId),
                    name = it.name
                )
            },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun statesOf(entity: BusinessPartnerDb, businessPartnerType: BusinessPartnerType) =
        entity.states.filter { it.businessPartnerTyp == businessPartnerType }.map(::toRecord)

    private fun toRecord(identifier: IdentifierDb) =
        IdentifierRaw(type = identifier.type, value = identifier.value, issuingBody = identifier.issuingBody, businessPartnerType = identifier.businessPartnerType)

    private fun toRecord(state: StateDb) =
        StateRaw(validFrom = state.validFrom, validTo = state.validTo, type = state.type, businessPartnerType = state.businessPartnerTyp)

    private fun toRecord(confidence: ConfidenceCriteriaDb) =
        ConfidenceCriteriaRaw(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            numberOfSharingMembers = confidence.numberOfBusinessPartners,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt,
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt,
            confidenceLevel = confidence.confidenceLevel
        )
}
