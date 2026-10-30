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
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationSharingStateRecord
import org.springframework.stereotype.Component

/**
 * Maps a stored relation sharing state to its record.
 */
@Component
class RelationSharingStateRecordMapper {

    /**
     * Returns the record of the sharing state of [relation], which must have one.
     */
    fun toRecord(relation: RelationDb): RelationSharingStateRecord {
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
}
