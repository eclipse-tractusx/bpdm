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
package org.eclipse.tractusx.bpdm.gate.model.output.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.value.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StateRaw
import java.time.Instant

data class BusinessPartnerOutputRecord(
    val externalId: String,
    val externalSequenceTimestamp: Instant?,
    val nameParts: List<String>,
    val identifiers: List<IdentifierRaw>,
    val states: List<StateRaw>,
    val roles: List<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntity: LegalEntity,
    val site: Site?,
    val address: Address,
    val scriptVariants: List<BusinessPartnerValuesRaw.ScriptVariant>,
    val additionalSites: List<AdditionalSiteRaw>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    data class LegalEntity(
        val bpn: String,
        val legalName: String?,
        val shortName: String?,
        val legalForm: String?,
        val confidence: ConfidenceCriteriaRaw,
        val ownershipUltimate: Boolean?,
        val ultimateOwnerBpnl: String?,
        val states: List<StateRaw>,
        val goldenRecordRelations: List<GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )

    data class Site(
        val bpn: String,
        val name: String?,
        val confidence: ConfidenceCriteriaRaw,
        val states: List<StateRaw>,
        val goldenRecordRelations: List<GoldenRecordRelationRaw<SiteGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )

    data class Address(
        val bpn: String,
        val name: String?,
        val addressType: AddressType?,
        val physicalPostalAddress: BusinessPartnerValuesRaw.PhysicalPostalAddress?,
        val alternativePostalAddress: BusinessPartnerValuesRaw.AlternativePostalAddress?,
        val confidence: ConfidenceCriteriaRaw,
        val states: List<StateRaw>,
        val goldenRecordRelations: List<GoldenRecordRelationRaw<AddressGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )
}
