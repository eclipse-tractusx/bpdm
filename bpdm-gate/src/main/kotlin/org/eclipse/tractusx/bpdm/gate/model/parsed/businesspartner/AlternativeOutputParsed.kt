/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import java.time.Instant
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb

/** Validated output for a matching process in the caller's tenant, with all persistence references resolved. */
data class AlternativeOutputParsed(
    val partner: BusinessPartnerInputUpsertParsed,
    val sharingState: SharingStateDb,
    val storedOutput: BusinessPartnerDb?,
    val addressIdentifiers: List<IdentifierRaw>,
    val legalEntityConfidence: ConfidenceCriteriaRaw?,
    val siteConfidence: ConfidenceCriteriaRaw?,
    val addressConfidence: ConfidenceCriteriaRaw?,
    val legalEntityUpdatedAt: Instant?,
    val siteUpdatedAt: Instant?,
    val addressUpdatedAt: Instant?
)
