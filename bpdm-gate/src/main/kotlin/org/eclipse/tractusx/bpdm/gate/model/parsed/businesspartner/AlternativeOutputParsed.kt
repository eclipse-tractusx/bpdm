/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import java.time.Instant

data class AlternativeOutputParsed(
    val partner: BusinessPartnerInputUpsertParsed,
    val legalEntityConfidence: ConfidenceCriteriaRaw?,
    val siteConfidence: ConfidenceCriteriaRaw?,
    val addressConfidence: ConfidenceCriteriaRaw?,
    val legalEntityUpdatedAt: Instant?,
    val siteUpdatedAt: Instant?,
    val addressUpdatedAt: Instant?
)
