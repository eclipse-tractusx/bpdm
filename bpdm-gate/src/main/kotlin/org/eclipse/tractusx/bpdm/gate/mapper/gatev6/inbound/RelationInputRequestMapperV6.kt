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
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationValidityPeriod
import org.springframework.stereotype.Component

/**
 * Maps the relation input requests of the v6 API to the unparsed upsert model, without judging their content.
 */
@Component
class RelationInputRequestMapperV6 {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [entry].
     */
    fun toUpsertRequest(entry: RelationPutEntryV6): RelationInputUpsertRequest =
        RelationInputUpsertRequest(
            externalId = entry.externalId,
            relationType = entry.relationType,
            sourceExternalId = entry.businessPartnerSourceExternalId,
            targetExternalId = entry.businessPartnerTargetExternalId,
            validityPeriods = entry.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = null
        )

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [request].
     */
    fun toUpsertRequest(request: RelationPostRequest): RelationInputUpsertRequest =
        RelationInputUpsertRequest(
            externalId = request.externalId,
            relationType = request.relationType,
            sourceExternalId = request.businessPartnerSourceExternalId,
            targetExternalId = request.businessPartnerTargetExternalId,
            validityPeriods = emptyList(),
            reasonCode = null
        )
}
