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


package org.eclipse.tractusx.bpdm.gate.service.operation.relation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationChange
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertEntryResult
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertEntryResult.*
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertParsed
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Writes parsed relations into the input stage of their tenant.
 *
 * A relation created without an external ID is assigned a new random one.
 */
@Service
class RelationInputUpsertOperationService(
    private val relationStageRepository: RelationStageRepository,
    private val sharingStateOperation: RelationSharingStateOperationService,
    private val changelogCreateOperation: ChangelogCreateOperationService,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the input of each of [requests] for [tenantBpnl] and resets its sharing state, resubmits an
     * unchanged input whose sharing failed, or leaves an unchanged input as it is.
     */
    @Transactional
    fun upsert(requests: List<RelationInputUpsertParsed>, tenantBpnl: String): List<RelationInputUpsertEntryResult> =
        requests.map { upsert(it, tenantBpnl) }

    private fun upsert(request: RelationInputUpsertParsed, tenantBpnl: String): RelationInputUpsertEntryResult {
        val existing = request.existing
        val result = when {
            existing == null -> create(request, tenantBpnl)
            hasChanged(request, existing) -> update(request, existing)
            existing.relation.sharingState?.sharingStateType == RelationSharingStateType.Error -> resubmit(existing)
            else -> Unchanged(existing)
        }
        logDebug(result)
        return result
    }

    private fun create(request: RelationInputUpsertParsed, tenantBpnl: String): Created {
        val relation = RelationDb(externalId = request.externalId ?: UUID.randomUUID().toString(), tenantBpnL = tenantBpnl, sharingState = null, output = null)
        sharingStateOperation.reset(relation)

        val input = RelationStageDb(
            relation = relation,
            relationType = request.relationType,
            stage = StageType.Input,
            source = request.source,
            target = request.target,
            validityPeriods = request.validityPeriods.map(::toEntity).toMutableList(),
            reasonCode = request.reasonCode
        )
        persist(input, ChangelogType.CREATE)
        logChange(relation, PersistedChangeType.Created, RelationChange.Aspect.Input, RelationChange.Aspect.SharingState)
        return Created(input)
    }

    private fun update(request: RelationInputUpsertParsed, input: RelationStageDb): Updated {
        input.relationType = request.relationType
        input.source = request.source
        input.target = request.target
        input.validityPeriods = request.validityPeriods.map(::toEntity).toMutableList()
        input.reasonCode = request.reasonCode
        input.updatedAt = Instant.now()
        persist(input, ChangelogType.UPDATE)

        sharingStateOperation.reset(input.relation)
        logChange(input.relation, PersistedChangeType.Updated, RelationChange.Aspect.Input, RelationChange.Aspect.SharingState)
        return Updated(input)
    }

    private fun resubmit(input: RelationStageDb): Resubmitted {
        sharingStateOperation.reset(input.relation)
        logChange(input.relation, PersistedChangeType.Updated, RelationChange.Aspect.SharingState)
        return Resubmitted(input)
    }

    // Compares the endpoints by external ID because the stored ones may be lazy proxies.
    private fun hasChanged(request: RelationInputUpsertParsed, input: RelationStageDb) =
        request.relationType != input.relationType ||
                request.source.externalId != input.source.externalId ||
                request.target.externalId != input.target.externalId ||
                request.validityPeriods != input.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) } ||
                request.reasonCode != input.reasonCode

    private fun persist(input: RelationStageDb, changeType: ChangelogType) {
        val relation = input.relation
        relationStageRepository.save(input)
        changelogCreateOperation.record(ChangelogEntryDb(relation.externalId, relation.tenantBpnL, changeType, StageType.Input, GoldenRecordType.Relation))
    }

    private fun logChange(relation: RelationDb, type: PersistedChangeType, vararg aspects: RelationChange.Aspect) =
        aspects.forEach { changeLogger.log(RelationChange(type, relation.externalId, relation.tenantBpnL, it)) }

    private fun logDebug(result: RelationInputUpsertEntryResult) =
        logger.debug {
            val identifier = result.input.relation.externalId
            when (result) {
                is Created -> "Created relation input $identifier"
                is Updated -> "Updated relation input $identifier"
                is Resubmitted -> "Resubmitted unchanged relation input $identifier after a sharing error"
                is Unchanged -> "Left unchanged relation input $identifier as it is"
            }
        }

    private fun toEntity(period: RelationValidityPeriod) = RelationValidityPeriodDb(validFrom = period.validFrom, validTo = period.validTo)
}
