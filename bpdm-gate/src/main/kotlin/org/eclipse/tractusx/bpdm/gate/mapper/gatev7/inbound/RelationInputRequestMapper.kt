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


package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound

import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationValidityPeriod
import org.springframework.stereotype.Component

/**
 * Maps the relation input entry of the API to the unparsed upsert model, without judging its content.
 */
@Component
class RelationInputRequestMapper(
    private val enumRequestMapper: EnumRequestMapper
) {

    /**
     * Returns the unparsed upsert entry carrying exactly the content of [entry].
     */
    fun toUpsertRequest(entry: RelationPutEntry): RelationInputUpsertRequest =
        RelationInputUpsertRequest(
            externalId = entry.externalId,
            relationType = enumRequestMapper.toModel(entry.relationType),
            sourceExternalId = entry.businessPartnerSourceExternalId,
            targetExternalId = entry.businessPartnerTargetExternalId,
            validityPeriods = entry.validityPeriods.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = entry.reasonCode
        )
}
