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

package org.eclipse.tractusx.bpdm.gate.service.parser.task

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskPreparationError
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskPreparationParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides which relations staged for sharing are sent to the Orchestrator as golden record tasks.
 */
@Service
class RelationTaskPreparationParser(
    private val relationStageRepository: RelationStageRepository,
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    /**
     * Derives the golden record relation the Orchestrator receives for each of [relations], defers relations whose
     * endpoints are not yet shared, and rejects relations whose endpoints do not form a relation the golden record
     * supports.
     */
    @Transactional(readOnly = true)
    fun parse(relations: List<RelationDb>): List<ParseResult<RelationTaskPreparationParsed, RelationTaskPreparationError>> {
        val inputsByRelationId = relationStageRepository.findByRelationInAndStage(relations.toSet(), StageType.Input).associateBy { it.relation.id }
        val endpoints = inputsByRelationId.values.flatMap { listOf(it.source, it.target) }
        val outputsBySharingStateId = businessPartnerRepository.findBySharingStateInAndStage(endpoints, StageType.Output).associateBy { it.sharingState.id }

        return relations.map { relation ->
            val input = inputsByRelationId[relation.id]
                ?: return@map ParseResult.Success(RelationTaskPreparationParsed.Deferred(relation))
            parse(relation, input, outputsBySharingStateId[input.source.id], outputsBySharingStateId[input.target.id])
        }
    }

    private fun parse(
        relation: RelationDb,
        input: RelationStageDb,
        sourceOutput: BusinessPartnerDb?,
        targetOutput: BusinessPartnerDb?
    ): ParseResult<RelationTaskPreparationParsed, RelationTaskPreparationError> {
        if (sourceOutput == null || targetOutput == null)
            return ParseResult.Success(RelationTaskPreparationParsed.Deferred(relation))

        val sourceAddressType = sourceOutput.postalAddress.addressType
        val targetAddressType = targetOutput.postalAddress.addressType
        val kind = determineKind(sourceAddressType, targetAddressType, input.relationType)
            ?: return ParseResult.ofSingleFailure(
                RelationTaskPreparationError.CombinationUnsupported(relation, input.relationType, sourceAddressType, targetAddressType)
            )

        val sourceBpn = kind.bpnOf(sourceOutput)
        val targetBpn = kind.bpnOf(targetOutput)
        if (sourceBpn.isNullOrBlank() || targetBpn.isNullOrBlank())
            return ParseResult.ofSingleFailure(RelationTaskPreparationError.BpnMissing(relation, kind.bpnType, sourceBpn, targetBpn))

        val sharingState = checkNotNull(relation.sharingState)
        val candidate = RelationTaskCandidateParsed(
            relationId = relation.id,
            externalId = relation.externalId,
            sharingStateUpdatedAt = sharingState.updatedAt,
            recordId = sharingState.recordId,
            relationType = input.relationType,
            sourceBpn = sourceBpn,
            targetBpn = targetBpn,
            validityPeriods = input.validityPeriods.map { RelationValidityPeriodRaw(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = input.reasonCode
        )
        return ParseResult.Success(RelationTaskPreparationParsed.Sendable(relation, candidate))
    }

    private fun determineKind(sourceAddressType: AddressType?, targetAddressType: AddressType?, relationType: RelationType): RelationTaskKind? {
        if (sourceAddressType == null || targetAddressType == null) return null

        val bothLegalEntityLike = isLegalEntityLike(sourceAddressType) && isLegalEntityLike(targetAddressType)

        return when (relationType) {
            RelationType.IsAlternativeHeadquarterFor,
            RelationType.IsOwnedBy,
            RelationType.IsManagedBy -> if (bothLegalEntityLike) RelationTaskKind.LegalEntity else null

            // An address succession is what remains once neither higher level claims the pair: every refined record has
            // a BPNA, so any other IsReplacedBy pair is a succession between the two addresses themselves.
            RelationType.IsReplacedBy -> when {
                bothLegalEntityLike -> RelationTaskKind.LegalEntity
                isSiteMainOnly(sourceAddressType) && isSiteMainOnly(targetAddressType) -> RelationTaskKind.Site
                else -> RelationTaskKind.Address
            }
        }
    }

    private fun isLegalEntityLike(addressType: AddressType) =
        addressType == AddressType.LegalAndSiteMainAddress || addressType == AddressType.LegalAddress

    // A site relation requires SiteMainAddress on both sides rather than everything a site can be reached through:
    // LegalAndSiteMainAddress is a legal entity too, and IsReplacedBy between two of those already means the legal
    // entities succeed each other. A site sharing its legal entity's address can therefore not be replaced.
    private fun isSiteMainOnly(addressType: AddressType) = addressType == AddressType.SiteMainAddress

    private enum class RelationTaskKind(val bpnType: String, val bpnOf: (BusinessPartnerDb) -> String?) {
        LegalEntity("BPNL", { it.bpnL }),
        Site("BPNS", { it.bpnS }),
        Address("BPNA", { it.bpnA })
    }
}
