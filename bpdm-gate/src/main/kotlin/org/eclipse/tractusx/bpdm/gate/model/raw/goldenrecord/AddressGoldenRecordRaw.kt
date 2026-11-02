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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import java.time.Instant

data class AddressGoldenRecordRaw(
    val bpn: String,
    val name: String?,
    val addressType: AddressType?,
    val identifiers: List<IdentifierRaw>,
    val states: List<StateRaw>,
    val physicalPostalAddress: PhysicalPostalAddressRaw,
    val alternativePostalAddress: AlternativeAddressRaw?,
    val confidence: ConfidenceCriteriaRaw,
    val relations: List<GoldenRecordRelationRaw<AddressGoldenRecordRelationType>>,
    val siteBpn: String?,
    val additionalSiteBpns: List<String>,
    val scriptVariants: List<ScriptVariant>,
    val updatedAt: Instant
) {
    data class ScriptVariant(
        val scriptCode: String,
        val name: String?,
        val physicalAddress: BusinessPartnerValuesRaw.PhysicalAddressScriptVariant,
        val alternativeAddressCity: String?
    )
}
