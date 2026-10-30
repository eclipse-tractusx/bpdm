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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputRecord
import org.springframework.stereotype.Component

/**
 * Maps a relation input record to the relation the API returns.
 */
@Component
class RelationInputDtoMapper(
    private val enumDtoMapper: EnumDtoMapper
) {

    /**
     * Returns the API relation for [record].
     */
    fun toDto(record: RelationInputRecord): RelationDto =
        RelationDto(
            externalId = record.externalId,
            relationType = enumDtoMapper.toDto(record.relationType),
            businessPartnerSourceExternalId = record.sourceExternalId,
            businessPartnerTargetExternalId = record.targetExternalId,
            validityPeriods = record.validityPeriods.map { RelationValidityPeriodDto(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = record.reasonCode,
            updatedAt = record.updatedAt,
            createdAt = record.createdAt
        )
}
