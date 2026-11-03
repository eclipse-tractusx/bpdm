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

import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationValidityPeriodRaw

/**
 * A relation input upsert entry of one tenant.
 *
 * Guarantees:
 * - [existing] is the input-stage relation of the tenant with [externalId], or null if the tenant has none.
 * - [source] and [target] are different business partners of the tenant.
 * - Every validity period ending at a `validTo` starts before it.
 */
data class RelationInputUpsertParsed(
    val externalId: String?,
    val existing: RelationStageDb?,
    val relationType: RelationType,
    val source: SharingStateDb,
    val target: SharingStateDb,
    val validityPeriods: List<RelationValidityPeriodRaw>,
    val reasonCode: String?
)
