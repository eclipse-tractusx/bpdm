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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPostRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.model.request.RelationPutEntryV6
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7InboundMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputUpsertRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.springframework.stereotype.Component

/**
 * Maps API v6 requests to their raw model.
 */
@Component
class ApiV6InboundMapper(
    private val apiV7InboundMapper: ApiV7InboundMapper
) {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [entry].
     */
    fun toUpsertRequest(entry: RelationPutEntryV6): RelationInputUpsertRaw =
        RelationInputUpsertRaw(
            externalId = entry.externalId,
            relationType = apiV7InboundMapper.toModel(entry.relationType),
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
            relationType = apiV7InboundMapper.toModel(request.relationType),
            sourceExternalId = request.businessPartnerSourceExternalId,
            targetExternalId = request.businessPartnerTargetExternalId,
            validityPeriods = emptyList(),
            reasonCode = null
        )
}
