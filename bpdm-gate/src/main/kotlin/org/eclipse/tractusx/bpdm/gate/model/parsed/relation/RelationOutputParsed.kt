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

package org.eclipse.tractusx.bpdm.gate.model.parsed.relation

import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType

/**
 * The golden record relation a relation output takes on.
 *
 * Guarantees:
 * - [sourceBpn] and [targetBpn] differ.
 * - Every validity period ending at a `validTo` starts before it.
 */
data class RelationOutputParsed(
    val relation: RelationDb,
    val relationType: SharableRelationType,
    val sourceBpn: String,
    val targetBpn: String,
    val validityPeriods: List<RelationValidityPeriodRaw>,
    val reasonCode: String?
)
