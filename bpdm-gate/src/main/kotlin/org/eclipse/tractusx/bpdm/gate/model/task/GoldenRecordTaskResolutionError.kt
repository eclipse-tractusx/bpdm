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

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb

/**
 * Reason a sharing state pending on a finished golden record task cannot take on the outcome of its task.
 */
sealed interface GoldenRecordTaskResolutionError {
    val sharingState: SharingStateDb
    val message: String?

    data class TaskMissing(override val sharingState: SharingStateDb) : GoldenRecordTaskResolutionError {
        override val message = "Missing Task in Orchestrator"
    }

    data class InputMissing(override val sharingState: SharingStateDb) : GoldenRecordTaskResolutionError {
        override val message = "No input data found for the references business partner"
    }

    data class ResultIncomplete(override val sharingState: SharingStateDb, val missingValues: List<String>) : GoldenRecordTaskResolutionError {
        override val message = "Task result lacks mandatory values: ${missingValues.joinToString()}".take(255)
    }
}
