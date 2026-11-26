/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class OutputChangelogSearchV7Request(
    @get:Schema(
        description = "Only changelog entries created after this time. Ignored if empty.",
        example = "2023-03-20T10:23:28.194Z"
    )
    val timestampAfter: Instant? = null,

    @get:ArraySchema(arraySchema = Schema(description = "Only for business partners with the given array of external IDs. Ignored if empty."))
    val externalIds: Set<String>? = emptySet(),

    @get:Schema(description = "Restrict output changelog entries to these alternative processes. Absent or empty selects only the golden record process.")
    val processes: Set<String>? = null
)
