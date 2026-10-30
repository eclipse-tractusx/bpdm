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

import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode

/**
 * The outcome a relation takes on from its finished golden record task.
 *
 * Guarantees:
 * - [relation] is pending on a task the Orchestrator reports as finished.
 */
sealed interface RelationTaskResolutionParsed {
    val relation: RelationDb

    data class Succeeded(
        override val relation: RelationDb,
        val output: RelationOutputParsed
    ) : RelationTaskResolutionParsed

    data class Failed(
        override val relation: RelationDb,
        val errorCode: RelationSharingStateErrorCode,
        val errorMessage: String
    ) : RelationTaskResolutionParsed
}
