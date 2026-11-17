/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner

import java.time.Instant

/** Unvalidated alternative output, including whether the caller supplied a site component. */
data class AlternativeOutputRaw(
    val partner: BusinessPartnerInputUpsertRaw,
    val hasSite: Boolean,
    val legalEntityConfidence: ConfidenceCriteriaRaw?,
    val siteConfidence: ConfidenceCriteriaRaw?,
    val addressConfidence: ConfidenceCriteriaRaw?,
    val legalEntityUpdatedAt: Instant?,
    val siteUpdatedAt: Instant?,
    val addressUpdatedAt: Instant?
)
