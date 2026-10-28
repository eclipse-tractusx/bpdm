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

package org.eclipse.tractusx.bpdm.gate.model.goldenrecord

import org.eclipse.tractusx.bpdm.gate.model.upsert.output.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import java.time.Instant

data class SiteGoldenRecordRequest(
    val bpn: String,
    val name: String,
    val states: List<State>,
    val confidence: ConfidenceCriteria,
    val relations: List<GoldenRecordRelation<SiteGoldenRecordRelationType>>,
    val scriptVariants: List<ScriptVariant>,
    val updatedAt: Instant
) {
    data class ScriptVariant(
        val scriptCode: String,
        val name: String
    )
}
