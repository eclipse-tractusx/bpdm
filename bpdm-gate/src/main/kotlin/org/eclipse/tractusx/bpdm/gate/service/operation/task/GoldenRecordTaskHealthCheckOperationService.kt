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

package org.eclipse.tractusx.bpdm.gate.service.operation.task

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckParsed
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Fails the business partners pending on a golden record task the Orchestrator no longer knows.
 */
@Service
class GoldenRecordTaskHealthCheckOperationService(
    private val sharingStateOperation: SharingStateOperationService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Fails each sharing state in [checks] still pending on the task it was checked for, if the Orchestrator does not
     * know that task.
     */
    @Transactional
    fun failLostTasks(checks: List<GoldenRecordTaskHealthCheckParsed>) {
        val lost = checks.filter { !it.isTaskKnown && isStillPendingOnCheckedTask(it) }.map { it.sharingState }
        logger.debug { "Checked the tasks of ${checks.size} pending business partners, ${lost.size} of them lost by the Orchestrator" }

        lost.forEach { sharingStateOperation.fail(it, BusinessPartnerSharingError.MissingTaskID, TASK_MISSING_MESSAGE) }
    }

    private fun isStillPendingOnCheckedTask(check: GoldenRecordTaskHealthCheckParsed): Boolean =
        check.sharingState.sharingStateType == SharingStateType.Pending && check.sharingState.taskId == check.checkedTaskId

    companion object {
        private const val TASK_MISSING_MESSAGE = "Missing Task in Orchestrator"
    }
}
