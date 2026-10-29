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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev6.outbound

import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.EnumDtoMapper
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateRecord
import org.springframework.stereotype.Component

/**
 * Maps a sharing state record to the sharing state response of the v6 API.
 */
@Component
class SharingStateDtoMapperV6(
    private val enumDtoMapper: EnumDtoMapper
) {

    /**
     * Returns the v6 API response for [record].
     */
    fun toDto(record: SharingStateRecord): SharingStateDto =
        SharingStateDto(
            externalId = record.externalId,
            sharingStateType = enumDtoMapper.toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(enumDtoMapper::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            sharingProcessStarted = record.sharingProcessStarted,
            taskId = record.taskId
        )
}
