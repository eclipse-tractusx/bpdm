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

package org.eclipse.tractusx.bpdm.gate.service.parser.relation

import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputSearchParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationInputSearchRaw
import org.springframework.stereotype.Service

/**
 * Turns loose relation input search criteria into the normalized form the search queries with.
 */
@Service
class RelationInputSearchParser {

    /**
     * Returns [request] normalized, with an absent or empty external ID, source or target filter meaning no filter.
     */
    fun parse(request: RelationInputSearchRaw): RelationInputSearchParsed =
        RelationInputSearchParsed(
            externalIds = request.externalIds?.takeIf { it.isNotEmpty() }?.toSet(),
            relationType = request.relationType,
            sourceExternalIds = request.sourceExternalIds?.takeIf { it.isNotEmpty() }?.toSet(),
            targetExternalIds = request.targetExternalIds?.takeIf { it.isNotEmpty() }?.toSet(),
            updatedAfter = request.updatedAfter
        )
}
