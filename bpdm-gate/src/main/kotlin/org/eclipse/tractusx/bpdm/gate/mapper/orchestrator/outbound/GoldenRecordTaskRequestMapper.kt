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

import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskResultStateSearchRequest
import org.springframework.stereotype.Component

/**
 * Maps business partner golden record task requests onto the requests the Orchestrator receives.
 */
@Component
class GoldenRecordTaskRequestMapper {

    /**
     * Maps [candidates] onto one request creating a golden record task from the sharing member side for each of them.
     */
    fun toCreateRequest(candidates: List<GoldenRecordTaskCandidate>): TaskCreateRequest =
        TaskCreateRequest(
            mode = TaskMode.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRequestEntry(recordId = it.recordId, businessPartner = it.businessPartner) }
        )

    /**
     * Maps [taskIds] onto a request asking for the result state of each of these tasks.
     */
    fun toResultStateSearchRequest(taskIds: List<String>): TaskResultStateSearchRequest =
        TaskResultStateSearchRequest(taskIds)
}
