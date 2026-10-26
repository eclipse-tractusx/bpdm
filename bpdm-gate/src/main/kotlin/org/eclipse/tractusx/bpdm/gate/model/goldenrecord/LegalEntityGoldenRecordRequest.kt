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
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.Identifier
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelation
import java.time.Instant

data class LegalEntityGoldenRecordRequest(
    val bpn: String,
    val legalName: String,
    val shortName: String?,
    val legalForm: String?,
    val identifiers: List<Identifier>,
    val states: List<State>,
    val confidence: ConfidenceCriteria,
    val relations: List<LegalEntityGoldenRecordRelation>,
    val ownershipUltimate: Boolean?,
    val ultimateOwnerBpnl: String?,
    val scriptVariants: List<ScriptVariant>,
    val updatedAt: Instant
) {
    data class ScriptVariant(
        val scriptCode: String,
        val legalName: String,
        val shortName: String?
    )
}
