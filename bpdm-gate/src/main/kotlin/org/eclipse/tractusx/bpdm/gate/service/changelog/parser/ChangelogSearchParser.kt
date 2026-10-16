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
package org.eclipse.tractusx.bpdm.gate.service.changelog.parser

import org.eclipse.tractusx.bpdm.gate.model.search.ChangelogSearchParsed
import org.eclipse.tractusx.bpdm.gate.model.search.ChangelogSearchRequest
import org.springframework.stereotype.Service

/**
 * Turns loose changelog search criteria into the normalized form the search queries with.
 *
 * Unlike the upsert parsers this one returns its parsed value directly instead of a `ParseResult`: no criterion can be
 * rejected, since an unknown external ID matches nothing.
 */
@Service
class ChangelogSearchParser {

    /**
     * Returns [request] normalized, with an absent or empty external ID filter meaning no filter.
     */
    fun parse(request: ChangelogSearchRequest): ChangelogSearchParsed =
        ChangelogSearchParsed(
            externalIds = request.externalIds?.takeIf { it.isNotEmpty() }?.toSet(),
            timestampAfter = request.timestampAfter
        )
}
