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


package org.eclipse.tractusx.bpdm.gate.mapper.gatev6.inbound

import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound.EnumRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Component

/**
 * Maps the relation input requests of the v6 API to the unparsed upsert model, without judging their content.
 */
@Component
class RelationInputRequestMapperV6(
    private val enumRequestMapper: EnumRequestMapper
) {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [entry].
     */
    fun toUpsertRequest(entry: RelationPutEntryV6): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = entry.externalId,
            relationType = enumRequestMapper.toModel(entry.relationType),
            sourceExternalId = entry.businessPartnerSourceExternalId,
            targetExternalId = entry.businessPartnerTargetExternalId,
            validityPeriods = entry.validityPeriods.map { RelationValidityPeriodRaw(it.validFrom, it.validTo) },
            reasonCode = null
        )

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertRequest(request: RelationPostRequest): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = request.externalId,
            relationType = enumRequestMapper.toModel(request.relationType),
            sourceExternalId = request.businessPartnerSourceExternalId,
            targetExternalId = request.businessPartnerTargetExternalId,
            validityPeriods = emptyList(),
            reasonCode = null
        )
}
