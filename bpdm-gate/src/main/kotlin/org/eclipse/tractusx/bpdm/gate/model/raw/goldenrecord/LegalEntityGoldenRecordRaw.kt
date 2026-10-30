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

package org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord

import org.eclipse.tractusx.bpdm.gate.model.raw.value.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StateRaw
import java.time.Instant

data class LegalEntityGoldenRecordRaw(
    val bpn: String,
    val legalName: String,
    val shortName: String?,
    val legalForm: String?,
    val identifiers: List<IdentifierRaw>,
    val states: List<StateRaw>,
    val confidence: ConfidenceCriteriaRaw,
    val relations: List<GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>>,
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
