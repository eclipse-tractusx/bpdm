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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationSharingStateRecord
import org.springframework.stereotype.Component

/**
 * Maps a relation sharing state record to the relation sharing state the API returns.
 */
@Component
class RelationSharingStateDtoMapper(
    private val enumDtoMapper: EnumDtoMapper
) {

    /**
     * Returns the API relation sharing state for [record].
     */
    fun toDto(record: RelationSharingStateRecord): RelationSharingStateDto =
        RelationSharingStateDto(
            externalId = record.externalId,
            sharingStateType = enumDtoMapper.toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(enumDtoMapper::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            taskId = record.taskId,
            updatedAt = record.updatedAt
        )
}
