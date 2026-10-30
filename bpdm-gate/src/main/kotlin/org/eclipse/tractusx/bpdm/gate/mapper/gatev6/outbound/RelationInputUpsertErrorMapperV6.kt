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

import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationAlreadyExistsException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.RelationInputUpsertErrorMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.springframework.stereotype.Component

/**
 * Maps a rejected relation input upsert to the error a v6 relation endpoint raises.
 */
@Component
class RelationInputUpsertErrorMapperV6(
    private val errorMapper: RelationInputUpsertErrorMapper
) {

    /**
     * Returns the conflict for a relation that already exists if [errors] name one, and the bad request reporting every
     * error otherwise.
     */
    fun toException(errors: List<RelationInputUpsertError>): RuntimeException =
        errors.filterIsInstance<RelationInputUpsertError.AlreadyExists>().firstOrNull()
            ?.let { BpdmRelationAlreadyExistsException(it.externalId) }
            ?: BpdmRelationInputUpsertException(errors.map(errorMapper::toMessage))
}
