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

package org.eclipse.tractusx.bpdm.gate.model.parsed.task

import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import java.time.Instant

/**
 * A relation staged for sharing, as the golden record relation the Orchestrator receives for it.
 *
 * Guarantees:
 * - Both endpoints are shared, and their address types and [relationType] form a relation the golden record supports.
 * - [sourceBpn] and [targetBpn] are non-blank BPNs of the level that combination selects: BPNL, BPNS or BPNA.
 */
data class RelationTaskCandidateParsed(
    val relationId: Long,
    val externalId: String,
    val sharingStateUpdatedAt: Instant,
    val recordId: String?,
    val relationType: RelationType,
    val sourceBpn: String,
    val targetBpn: String,
    val validityPeriods: List<RelationValidityPeriodRaw>,
    val reasonCode: String?
)
