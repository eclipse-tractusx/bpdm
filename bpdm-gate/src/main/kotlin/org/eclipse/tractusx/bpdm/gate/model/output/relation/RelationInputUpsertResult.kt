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


package org.eclipse.tractusx.bpdm.gate.model.output.relation

import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError

/**
 * Result of upserting a batch of relation inputs: either everything written or nothing, with every reason.
 */
sealed interface RelationInputUpsertResult {
    data class Written(val relations: List<RelationInputRecord>) : RelationInputUpsertResult
    data class Rejected(val errors: List<RelationInputUpsertError>) : RelationInputUpsertResult
}
