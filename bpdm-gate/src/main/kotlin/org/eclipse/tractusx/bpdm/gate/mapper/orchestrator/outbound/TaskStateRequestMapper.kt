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

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.gate.model.raw.task.TaskReferenceRaw
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.springframework.stereotype.Component

/**
 * Maps references to golden record tasks of any kind onto the task state requests the Orchestrator receives.
 */
@Component
class TaskStateRequestMapper {

    /**
     * Maps [tasks] onto a request asking for the current state of each of these tasks.
     */
    fun toRequest(tasks: List<TaskReferenceRaw>): TaskStateRequest =
        TaskStateRequest(tasks.map { TaskStateRequest.Entry(taskId = it.taskId, recordId = it.recordId) })
}
