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

import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputDeleteException
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteError.NotFound
import org.springframework.stereotype.Component

/**
 * Maps a rejected relation input delete to the error the v6 relation endpoint raises.
 */
@Component
class RelationInputDeleteErrorMapperV6 {

    /**
     * Returns the bad request reporting every error in [errors].
     */
    fun toException(errors: List<RelationInputDeleteError>): RuntimeException =
        BpdmRelationInputDeleteException(errors.map(::toMessage))

    private fun toMessage(error: RelationInputDeleteError): String =
        when (error) {
            is NotFound -> "Relation '${error.externalId}' can not be found."
        }
}
