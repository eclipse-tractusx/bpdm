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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationOutputRecord
import org.springframework.stereotype.Component

/**
 * Maps a relation output record to the relation output the API returns.
 */
@Component
class RelationOutputDtoMapper(
    private val enumDtoMapper: EnumDtoMapper
) {

    /**
     * Returns the API relation output for [record].
     */
    fun toDto(record: RelationOutputRecord): RelationOutputDto =
        RelationOutputDto(
            externalId = record.externalId,
            relationType = enumDtoMapper.toDto(record.relationType),
            sourceBpn = record.sourceBpn,
            targetBpn = record.targetBpn,
            validityPeriods = record.validityPeriods.map { RelationValidityPeriodDto(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = record.reasonCode,
            updatedAt = record.updatedAt
        )
}
