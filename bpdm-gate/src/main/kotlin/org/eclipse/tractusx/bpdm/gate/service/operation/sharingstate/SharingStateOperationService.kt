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

package org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.task.TaskReference
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.service.SharingStateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Moves the sharing states of business partners through their lifecycle.
 */
@Service
class SharingStateOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val sharingStateService: SharingStateService,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Returns a reference to the task of each sharing state still pending on any of [taskIds].
     */
    @Transactional(readOnly = true)
    fun findPendingTasks(taskIds: Collection<String>): List<TaskReference> =
        sharingStateRepository.findByTaskIdIn(taskIds.toSet())
            .filter { it.sharingStateType == SharingStateType.Pending }
            .map { TaskReference(taskId = it.taskId.toString(), recordId = it.orchestratorRecordId.toString()) }

    /**
     * Marks each of [sharingStates] as successfully shared.
     */
    @Transactional
    fun succeed(sharingStates: List<SharingStateDb>) {
        sharingStates.forEach { sharingState ->
            sharingStateService.setSuccess(sharingState)
            logChange(sharingState)
            logger.debug { "Resolved golden record task ${sharingState.taskId} of business partner ${sharingState.externalId} as success" }
        }
    }

    /**
     * Marks [sharingState] as failed with [errorCode].
     */
    @Transactional
    fun fail(sharingState: SharingStateDb, errorCode: BusinessPartnerSharingError, errorMessage: String?) {
        sharingStateService.setError(sharingState, errorCode, errorMessage)
        logChange(sharingState)
        logger.debug { "Resolved golden record task ${sharingState.taskId} of business partner ${sharingState.externalId} as error $errorCode" }
    }

    private fun logChange(sharingState: SharingStateDb) {
        changeLogger.log(
            BusinessPartnerChange(PersistedChangeType.Updated, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.SharingState)
        )
    }
}
