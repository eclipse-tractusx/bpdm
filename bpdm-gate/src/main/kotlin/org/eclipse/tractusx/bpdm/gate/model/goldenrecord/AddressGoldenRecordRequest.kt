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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.AlternativeAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.Identifier
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelation
import java.time.Instant

data class AddressGoldenRecordRequest(
    val bpn: String,
    val name: String?,
    val addressType: AddressType?,
    val identifiers: List<Identifier>,
    val states: List<State>,
    val physicalPostalAddress: PhysicalPostalAddress,
    val alternativePostalAddress: AlternativeAddress?,
    val confidence: ConfidenceCriteria,
    val relations: List<AddressGoldenRecordRelation>,
    val siteBpn: String?,
    val additionalSiteBpns: List<String>,
    val scriptVariants: List<ScriptVariant>,
    val updatedAt: Instant
) {
    data class ScriptVariant(
        val scriptCode: String,
        val name: String?,
        val physicalAddress: BusinessPartnerValues.PhysicalAddressScriptVariant,
        val alternativeAddressCity: String?
    )
}
