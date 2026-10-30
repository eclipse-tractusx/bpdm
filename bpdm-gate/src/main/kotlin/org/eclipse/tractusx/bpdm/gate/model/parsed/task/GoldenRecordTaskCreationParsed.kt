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

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import java.util.UUID

/**
 * How a business partner takes on the golden record task the Orchestrator created for it.
 *
 * Guarantees:
 * - [recordIdToAssign] is set only when [sharingState] holds no Orchestrator record ID yet.
 */
sealed interface GoldenRecordTaskCreationParsed {
    val sharingState: SharingStateDb
    val recordIdToAssign: UUID?

    /**
     * The task starts sharing the business partner.
     *
     * Guarantees:
     * - [sharingState] is still ready to be shared, with the Orchestrator record ID and input the task was created from.
     */
    data class Started(
        override val sharingState: SharingStateDb,
        val taskId: String,
        override val recordIdToAssign: UUID?
    ) : GoldenRecordTaskCreationParsed

    /**
     * The task was created from a state of the business partner it no longer has.
     *
     * Guarantees:
     * - [sharingState] is no longer ready to be shared, or its Orchestrator record ID or input changed since the task
     *   was created.
     */
    data class Stale(
        override val sharingState: SharingStateDb,
        override val recordIdToAssign: UUID?
    ) : GoldenRecordTaskCreationParsed
}
