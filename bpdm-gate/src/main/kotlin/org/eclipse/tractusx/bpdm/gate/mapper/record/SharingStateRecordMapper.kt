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

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateRecord
import org.springframework.stereotype.Component

/**
 * Maps a stored sharing state to its record.
 */
@Component
class SharingStateRecordMapper {

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
