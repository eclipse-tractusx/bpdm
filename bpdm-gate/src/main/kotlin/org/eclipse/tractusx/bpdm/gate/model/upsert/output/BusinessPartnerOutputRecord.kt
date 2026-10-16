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
package org.eclipse.tractusx.bpdm.gate.model.upsert.output

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.eclipse.tractusx.orchestrator.api.model.AddressGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.orchestrator.api.model.SiteGoldenRecordRelationType
import java.time.Instant

data class BusinessPartnerOutputRecord(
    val externalId: String,
    val externalSequenceTimestamp: Instant?,
    val nameParts: List<String>,
    val identifiers: List<Identifier>,
    val states: List<State>,
    val roles: List<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntity: LegalEntity,
    val site: Site?,
    val address: Address,
    val scriptVariants: List<BusinessPartnerValues.ScriptVariant>,
    val additionalSites: List<AdditionalSite>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    data class LegalEntity(
        val bpn: String,
        val legalName: String?,
        val shortName: String?,
        val legalForm: String?,
        val confidence: ConfidenceCriteria,
        val ownershipUltimate: Boolean?,
        val ultimateOwnerBpnl: String?,
        val states: List<State>,
        val goldenRecordRelations: List<GoldenRecordRelation<LegalEntityGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )

    data class Site(
        val bpn: String,
        val name: String?,
        val confidence: ConfidenceCriteria,
        val states: List<State>,
        val goldenRecordRelations: List<GoldenRecordRelation<SiteGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )

    data class Address(
        val bpn: String,
        val name: String?,
        val addressType: AddressType?,
        val physicalPostalAddress: BusinessPartnerValues.PhysicalPostalAddress?,
        val alternativePostalAddress: BusinessPartnerValues.AlternativePostalAddress?,
        val confidence: ConfidenceCriteria,
        val states: List<State>,
        val goldenRecordRelations: List<GoldenRecordRelation<AddressGoldenRecordRelationType>>,
        val updatedAt: Instant?
    )

    data class GoldenRecordRelation<T>(
        val type: T,
        val sourceBpn: String,
        val targetBpn: String
    )
}
