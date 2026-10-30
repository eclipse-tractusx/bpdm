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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.AdditionalSiteDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerInputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationSharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputDesignationParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Component

/**
 * Maps persisted entities to the records handed back to controllers.
 */
@Component
class DbToOutputMapper(
    private val dbToParsedMapper: DbToParsedMapper
) {

    /**
     * Returns the record of [entity].
     */
    fun toRecord(entity: BusinessPartnerDb): BusinessPartnerInputRecord =
        BusinessPartnerInputRecord(
            externalId = entity.sharingState.externalId,
            externalSequenceTimestamp = entity.externalSequenceTimestamp,
            content = dbToParsedMapper.toContent(entity),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    /**
     * Returns the record of [designation]'s output, showing its designated identifiers and states as the business
     * partner's own.
     */
    fun toRecord(designation: BusinessPartnerOutputDesignationParsed): BusinessPartnerOutputRecord {
        val (entity, identifiers, states) = designation
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
                confidence = dbToParsedMapper.toConfidence(entity.legalEntityConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing legal entity confidence criteria")),
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
                    confidence = dbToParsedMapper.toConfidence(entity.siteConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing site confidence criteria")),
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
                physicalPostalAddress = entity.postalAddress.physicalPostalAddress?.let(dbToParsedMapper::toValue),
                alternativePostalAddress = entity.postalAddress.alternativePostalAddress?.let(dbToParsedMapper::toValue),
                confidence = dbToParsedMapper.toConfidence(entity.addressConfidence ?: throw BpdmInvalidPartnerException(externalId, "Missing address confidence criteria")),
                states = statesOf(entity, BusinessPartnerType.ADDRESS),
                goldenRecordRelations = entity.addressGoldenRecordRelations.map {
                    GoldenRecordRelationRaw(it.relationType, it.sourceBpn, it.targetBpn)
                },
                updatedAt = entity.addressUpdatedAt
            ),
            scriptVariants = entity.scriptVariants.map(dbToParsedMapper::toValue),
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

    /**
     * Returns the record of [input].
     */
    fun toRecord(input: RelationStageDb): RelationInputRecord =
        RelationInputRecord(
            externalId = input.relation.externalId,
            relationType = input.relationType,
            sourceExternalId = input.source.externalId,
            targetExternalId = input.target.externalId,
            validityPeriods = input.validityPeriods.map { RelationValidityPeriodRaw(it.validFrom, it.validTo) },
            reasonCode = input.reasonCode,
            createdAt = input.createdAt,
            updatedAt = input.updatedAt
        )

    /**
     * Returns the record of the output of [relation], which must have one.
     */
    fun toOutputRecord(relation: RelationDb): RelationOutputRecord {
        val output = checkNotNull(relation.output) { "Relation '${relation.externalId}' has no output" }
        return RelationOutputRecord(
            externalId = relation.externalId,
            relationType = output.relationType,
            sourceBpn = output.sourceBpn,
            targetBpn = output.targetBpn,
            validityPeriods = output.validityPeriods.map { RelationValidityPeriodRaw(it.validFrom, it.validTo) },
            reasonCode = output.reasonCode,
            updatedAt = output.resultUpdatedAt
        )
    }

    /**
     * Returns the record of the sharing state of [relation], which must have one.
     */
    fun toSharingStateRecord(relation: RelationDb): RelationSharingStateRecord {
        val sharingState = checkNotNull(relation.sharingState) { "Relation '${relation.externalId}' has no sharing state" }
        return RelationSharingStateRecord(
            externalId = relation.externalId,
            sharingStateType = sharingState.sharingStateType,
            sharingErrorCode = sharingState.sharingErrorCode,
            sharingErrorMessage = sharingState.sharingErrorMessage,
            taskId = sharingState.taskId,
            updatedAt = sharingState.updatedAt
        )
    }

    /**
     * Returns the record of [entity].
     */
    fun toRecord(entity: SharingStateDb): SharingStateRecord =
        SharingStateRecord(
            externalId = entity.externalId,
            sharingStateType = entity.sharingStateType,
            sharingErrorCode = entity.sharingErrorCode,
            sharingErrorMessage = entity.sharingErrorMessage,
            sharingProcessStarted = entity.sharingProcessStarted,
            taskId = entity.taskId,
            updatedAt = entity.updatedAt
        )
}
