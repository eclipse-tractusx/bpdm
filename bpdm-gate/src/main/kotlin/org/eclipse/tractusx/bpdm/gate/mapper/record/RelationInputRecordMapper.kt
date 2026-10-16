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

import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationValidityPeriod
import org.springframework.stereotype.Component

/**
 * Maps a stored relation input to its record.
 */
@Component
class RelationInputRecordMapper {

    /**
     * Returns the record of [input].
     */
    fun toRecord(input: RelationStageDb): RelationInputRecord =
        RelationInputRecord(
            externalId = input.relation.externalId,
            relationType = input.relationType,
            sourceExternalId = input.source.externalId,
            targetExternalId = input.target.externalId,
            validityPeriods = input.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = input.reasonCode,
            createdAt = input.createdAt,
            updatedAt = input.updatedAt
        )
}
