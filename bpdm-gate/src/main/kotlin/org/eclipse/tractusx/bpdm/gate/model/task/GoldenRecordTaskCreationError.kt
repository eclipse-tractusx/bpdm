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

/**
 * Reason a golden record task the Orchestrator created cannot be attached to the business partner it was created for.
 */
sealed interface GoldenRecordTaskCreationError {
    val externalId: String
    val taskId: String

    data class SharingStateMissing(override val externalId: String, override val taskId: String) : GoldenRecordTaskCreationError

    data class RecordIdInvalid(override val externalId: String, override val taskId: String, val recordId: String) : GoldenRecordTaskCreationError
}
