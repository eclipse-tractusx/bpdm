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
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.entity.RelationOutputDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationChangeRaw
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogCreateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Writes the output stage of relations.
 */
@Service
class RelationOutputOperationService(
    private val relationRepository: RelationRepository,
    private val changelogCreateOperation: ChangelogCreateOperationService,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or replaces the output of each of [outputs].
     */
    @Transactional
    fun write(outputs: List<RelationOutputParsed>) {
        outputs.forEach(::write)
    }

    private fun write(output: RelationOutputParsed) {
        val relation = output.relation
        val changelogType = if (relation.output == null) ChangelogType.CREATE else ChangelogType.UPDATE

        relation.output = RelationOutputDb(
            relationType = output.relationType,
            sourceBpn = output.sourceBpn,
            targetBpn = output.targetBpn,
            resultUpdatedAt = Instant.now(),
            validityPeriods = output.validityPeriods.map { RelationValidityPeriodDb(validFrom = it.validFrom, validTo = it.validTo) }.toMutableList(),
            reasonCode = output.reasonCode
        )
        relationRepository.save(relation)
        changelogCreateOperation.record(ChangelogEntryDb(relation.externalId, relation.tenantBpnL, changelogType, StageType.Output, GoldenRecordType.Relation))

        val changeType = when (changelogType) {
            ChangelogType.CREATE -> PersistedChangeType.Created
            ChangelogType.UPDATE -> PersistedChangeType.Updated
        }
        changeLogger.log(RelationChangeRaw(changeType, relation.externalId, relation.tenantBpnL, RelationChangeRaw.Aspect.Output))
        logger.debug { "Wrote relation output ${relation.externalId} (${output.sourceBpn} -> ${output.targetBpn}) as $changelogType" }
    }
}
