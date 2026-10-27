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

import org.eclipse.tractusx.bpdm.gate.entity.RelationDb

/**
 * Reason a relation pending on a finished golden record task cannot take on the outcome of its task.
 */
sealed interface RelationTaskResolutionError {
    val relation: RelationDb
    val message: String

    data class TaskMissing(override val relation: RelationDb) : RelationTaskResolutionError {
        override val message = "Missing Task in Orchestrator"
    }

    data class ResultUnusable(override val relation: RelationDb, override val message: String) : RelationTaskResolutionError
}
