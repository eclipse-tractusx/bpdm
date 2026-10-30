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

import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Component

/**
 * Maps a stored relation output to its record.
 */
@Component
class RelationOutputRecordMapper {

    /**
     * Returns the record of the output of [relation], which must have one.
     */
    fun toRecord(relation: RelationDb): RelationOutputRecord {
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
}
