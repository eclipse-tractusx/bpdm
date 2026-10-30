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

import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import java.time.Instant

data class RelationTaskCandidate(
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
