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


package org.eclipse.tractusx.bpdm.gate.service.parser.relation

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertError.*
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Turns a batch of relation input upsert entries into their parsed form, or into every rule violation it contains.
 */
@Service
class RelationInputUpsertParser(
    private val relationStageRepository: RelationStageRepository,
    private val sharingStateRepository: SharingStateRepository
) {

    /**
     * Parses [requests] of [tenantBpnl] as a whole, succeeding only if every entry is valid and failing with the
     * violations of every entry otherwise; an entry naming a relation the tenant does not have is a violation unless
     * [createIfNotExist] is set.
     */
    @Transactional(readOnly = true)
    fun parse(
        requests: List<RelationInputUpsertRequest>,
        tenantBpnl: String,
        createIfNotExist: Boolean
    ): ParseResult<List<RelationInputUpsertParsed>, RelationInputUpsertError> {
        val duplicates = requests.groupingBy { it.externalId }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) return ParseResult.ofSingleFailure(DuplicateExternalIds(duplicates))
        if (requests.isEmpty()) return ParseResult.Success(emptyList())

        val existingByExternalId = findExistingInputs(requests, tenantBpnl)
        val businessPartnersByExternalId = findBusinessPartners(requests, tenantBpnl)

        val results = requests.map { request ->
            zipParseResults(
                resolveExisting(request, existingByExternalId[request.externalId], createIfNotExist),
                resolveEndpoints(request, businessPartnersByExternalId),
                validateValidityPeriods(request)
            ) { existing, (source, target), validityPeriods ->
                RelationInputUpsertParsed(
                    externalId = request.externalId,
                    existing = existing,
                    relationType = request.relationType,
                    source = source,
                    target = target,
                    validityPeriods = validityPeriods,
                    reasonCode = request.reasonCode
                )
            }
        }

        val errors = results.filterIsInstance<ParseResult.Failure<RelationInputUpsertError>>().flatMap { it.errors }
        return if (errors.isEmpty())
            ParseResult.Success(results.map { (it as ParseResult.Success).parsed })
        else
            ParseResult.Failure(errors)
    }

    private fun findExistingInputs(requests: List<RelationInputUpsertRequest>, tenantBpnl: String): Map<String, RelationStageDb> =
        relationStageRepository.findAll(
            Specification.allOf(
                RelationStageRepository.Specs.byTenantBpnL(tenantBpnl),
                RelationStageRepository.Specs.byStage(StageType.Input),
                RelationStageRepository.Specs.byExternalIds(requests.map { it.externalId })
            )
        ).associateBy { it.relation.externalId }

    private fun findBusinessPartners(requests: List<RelationInputUpsertRequest>, tenantBpnl: String): Map<String, SharingStateDb> {
        val externalIds = requests.flatMap { listOf(it.sourceExternalId, it.targetExternalId) }.toSet()
        return sharingStateRepository.findByExternalIdInAndTenantBpnl(externalIds, tenantBpnl).associateBy { it.externalId }
    }

    private fun resolveExisting(
        request: RelationInputUpsertRequest,
        existing: RelationStageDb?,
        createIfNotExist: Boolean
    ): ParseResult<RelationStageDb?, RelationInputUpsertError> =
        if (existing == null && !createIfNotExist)
            ParseResult.ofSingleFailure(NotFound(request.externalId))
        else
            ParseResult.Success(existing)

    private fun resolveEndpoints(
        request: RelationInputUpsertRequest,
        businessPartnersByExternalId: Map<String, SharingStateDb>
    ): ParseResult<Pair<SharingStateDb, SharingStateDb>, RelationInputUpsertError> {
        val source = businessPartnersByExternalId[request.sourceExternalId]
        val target = businessPartnersByExternalId[request.targetExternalId]
        val errors = listOfNotNull(
            SourceNotFound(request.externalId, request.sourceExternalId).takeIf { source == null },
            TargetNotFound(request.externalId, request.targetExternalId).takeIf { target == null },
            SourceEqualsTarget(request.externalId, request.sourceExternalId).takeIf { request.sourceExternalId == request.targetExternalId }
        )
        return if (errors.isEmpty() && source != null && target != null)
            ParseResult.Success(source to target)
        else
            ParseResult.Failure(errors)
    }

    private fun validateValidityPeriods(request: RelationInputUpsertRequest): ParseResult<List<RelationValidityPeriod>, RelationInputUpsertError> {
        val errors = request.validityPeriods.mapNotNull { period ->
            period.validTo
                ?.takeIf { !period.validFrom.isBefore(it) }
                ?.let { InvalidValidityPeriod(request.externalId, period.validFrom, it) }
        }
        return if (errors.isEmpty()) ParseResult.Success(request.validityPeriods) else ParseResult.Failure(errors)
    }
}
