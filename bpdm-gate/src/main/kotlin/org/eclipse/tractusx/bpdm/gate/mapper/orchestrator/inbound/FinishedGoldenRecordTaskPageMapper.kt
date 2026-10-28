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

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.gate.model.task.FinishedGoldenRecordTaskPage
import org.eclipse.tractusx.orchestrator.api.model.FinishedTaskEventsResponse
import org.springframework.stereotype.Component

/**
 * Maps the pages of finished task events the Orchestrator reports onto the finished task pages the Gate resolves.
 */
@Component
class FinishedGoldenRecordTaskPageMapper {

    /**
     * Maps the first page of [events] onto its finished tasks, the finish time of its last event and whether more
     * pages follow.
     */
    fun toPage(events: FinishedTaskEventsResponse): FinishedGoldenRecordTaskPage =
        FinishedGoldenRecordTaskPage(
            taskIds = events.content.map { it.taskId },
            lastFinishedAt = events.content.lastOrNull()?.timestamp,
            hasMore = events.totalPages > 1
        )
}
