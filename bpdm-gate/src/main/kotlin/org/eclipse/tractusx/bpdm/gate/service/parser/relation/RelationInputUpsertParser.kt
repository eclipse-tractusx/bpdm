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
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError.*
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertBatchParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
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
    private val apiConfigProperties: ApiConfigProperties,
    private val relationStageRepository: RelationStageRepository,
    private val sharingStateRepository: SharingStateRepository
) {

    /**
     * Parses [requests] of [tenantBpnl] as a whole, succeeding only if every entry is valid and failing with the
     * violations of every entry otherwise; [mode] decides whether naming a relation the tenant does or does not have
     * yet is a violation.
     */
    @Transactional(readOnly = true)
    fun parse(
        requests: List<RelationInputUpsertRaw>,
        tenantBpnl: String,
        mode: RelationInputUpsertMode
    ): ParseResult<RelationInputUpsertBatchParsed, RelationInputUpsertError> {
        val limit = apiConfigProperties.upsertLimit
        if (requests.size > limit) return ParseResult.ofSingleFailure(UpsertLimitExceeded(limit, requests.size))
        val duplicates = requests.mapNotNull { it.externalId }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) return ParseResult.ofSingleFailure(DuplicateExternalIds(duplicates))
        if (requests.isEmpty()) return ParseResult.Success(RelationInputUpsertBatchParsed(tenantBpnl, emptyList()))

        val existingByExternalId = findExistingInputs(requests, tenantBpnl)
        val businessPartnersByExternalId = findBusinessPartners(requests, tenantBpnl)

        val results = requests.map { request ->
            zipParseResults(
                resolveExisting(request, existingByExternalId[request.externalId], mode),
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
            ParseResult.Success(RelationInputUpsertBatchParsed(tenantBpnl, results.map { (it as ParseResult.Success).parsed }))
        else
            ParseResult.Failure(errors)
    }

    private fun findExistingInputs(requests: List<RelationInputUpsertRaw>, tenantBpnl: String): Map<String, RelationStageDb> =
        relationStageRepository.findAll(
            Specification.allOf(
                RelationStageRepository.Specs.byTenantBpnL(tenantBpnl),
                RelationStageRepository.Specs.byStage(StageType.Input),
                RelationStageRepository.Specs.byExternalIds(requests.mapNotNull { it.externalId })
            )
        ).associateBy { it.relation.externalId }

    private fun findBusinessPartners(requests: List<RelationInputUpsertRaw>, tenantBpnl: String): Map<String, SharingStateDb> {
        val externalIds = requests.flatMap { listOf(it.sourceExternalId, it.targetExternalId) }.toSet()
        return sharingStateRepository.findByExternalIdInAndTenantBpnl(externalIds, tenantBpnl).associateBy { it.externalId }
    }

    private fun resolveExisting(
        request: RelationInputUpsertRaw,
        existing: RelationStageDb?,
        mode: RelationInputUpsertMode
    ): ParseResult<RelationStageDb?, RelationInputUpsertError> =
        when {
            existing == null && mode == RelationInputUpsertMode.UpdateOnly -> ParseResult.ofSingleFailure(NotFound(request.externalId))
            existing != null && mode == RelationInputUpsertMode.CreateOnly -> ParseResult.ofSingleFailure(AlreadyExists(existing.relation.externalId))
            else -> ParseResult.Success(existing)
        }

    private fun resolveEndpoints(
        request: RelationInputUpsertRaw,
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

    private fun validateValidityPeriods(request: RelationInputUpsertRaw): ParseResult<List<RelationValidityPeriodRaw>, RelationInputUpsertError> {
        val errors = request.validityPeriods.mapNotNull { period ->
            period.validTo
                ?.takeIf { !period.validFrom.isBefore(it) }
                ?.let { InvalidValidityPeriod(request.externalId, period.validFrom, it) }
        }
        return if (errors.isEmpty()) ParseResult.Success(request.validityPeriods) else ParseResult.Failure(errors)
    }
}
