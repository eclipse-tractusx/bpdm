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

package org.eclipse.tractusx.bpdm.gate.model.task

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordRelation
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SiteGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import java.time.Instant
import java.time.LocalDateTime

data class GoldenRecordTaskOutputRequest(
    val addressType: AddressType?,
    val owningCompanyBpnl: String?,
    val nameParts: List<String>,
    val identifiers: List<Identifier>,
    val states: List<State>,
    val legalEntityBpn: String?,
    val legalName: String?,
    val shortName: String?,
    val legalForm: String?,
    val siteBpn: String?,
    val siteName: String?,
    val addressBpn: String?,
    val addressName: String?,
    val physicalPostalAddress: BusinessPartnerValues.PhysicalPostalAddress?,
    val alternativePostalAddress: BusinessPartnerValues.AlternativePostalAddress?,
    val legalEntityConfidence: Confidence?,
    val siteConfidence: Confidence?,
    val addressConfidence: Confidence?,
    val legalEntityUpdatedAt: Instant?,
    val siteUpdatedAt: Instant?,
    val addressUpdatedAt: Instant?,
    val scriptVariants: List<BusinessPartnerValues.ScriptVariant>,
    val legalEntityGoldenRecordRelations: List<GoldenRecordRelation<LegalEntityGoldenRecordRelationType>>,
    val siteGoldenRecordRelations: List<GoldenRecordRelation<SiteGoldenRecordRelationType>>,
    val addressGoldenRecordRelations: List<GoldenRecordRelation<AddressGoldenRecordRelationType>>,
    val ownershipUltimate: Boolean?,
    val ultimateOwnerBpnl: String?,
    val additionalSites: List<BusinessPartnerValues.AdditionalSite>
) {
    data class Identifier(
        val type: String?,
        val value: String?,
        val issuingBody: String?,
        val businessPartnerType: BusinessPartnerType
    )

    data class State(
        val type: BusinessStateType?,
        val validFrom: LocalDateTime?,
        val validTo: LocalDateTime?,
        val businessPartnerType: BusinessPartnerType
    )

    data class Confidence(
        val sharedByOwner: Boolean?,
        val checkedByExternalDataSource: Boolean?,
        val numberOfSharingMembers: Int?,
        val lastConfidenceCheckAt: LocalDateTime?,
        val nextConfidenceCheckAt: LocalDateTime?,
        val confidenceLevel: Int?
    )
}
