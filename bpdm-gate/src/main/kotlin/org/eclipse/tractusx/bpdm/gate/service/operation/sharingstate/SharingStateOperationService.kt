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
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordBpns
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SharedBusinessPartner
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationParsed
import org.eclipse.tractusx.bpdm.gate.model.task.PendingGoldenRecordTask
import org.eclipse.tractusx.bpdm.gate.model.task.TaskReference
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Moves the sharing states of business partners through their lifecycle.
 */
@Service
class SharingStateOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Returns the sharing states of [externalIds] for [tenantBpnl] in the same order, creating each missing one in its
     * initial state.
     */
    @Transactional
    fun getOrCreate(externalIds: List<String>, tenantBpnl: String?): List<SharingStateDb> {
        val sharingStatesByExternalId = sharingStateRepository.findByExternalIdInAndTenantBpnl(externalIds, tenantBpnl).associateBy { it.externalId }
        return externalIds.map { externalId -> sharingStatesByExternalId[externalId] ?: create(externalId, tenantBpnl) }
    }

    /**
     * Resets [sharingState] to its initial state for a new sharing attempt, clearing any error and task of a previous one.
     */
    @Transactional
    fun reset(sharingState: SharingStateDb) {
        sharingState.sharingStateType = initialType()
        sharingState.sharingErrorCode = null
        sharingState.sharingErrorMessage = null
        sharingState.sharingProcessStarted = null
        sharingState.taskId = null
        sharingStateRepository.save(sharingState)
        logChange(sharingState)
    }

    /**
     * Returns a reference to the task of each sharing state still pending on any of [taskIds].
     */
    @Transactional(readOnly = true)
    fun findPendingTasks(taskIds: Collection<String>): List<TaskReference> =
        sharingStateRepository.findByTaskIdIn(taskIds.toSet())
            .filter { it.sharingStateType == SharingStateType.Pending }
            .map { TaskReference(taskId = it.taskId.toString(), recordId = it.orchestratorRecordId.toString()) }

    /**
     * Returns up to [limit] sharing states pending on a task, in the order of their id and starting after [sharingStateId].
     */
    @Transactional(readOnly = true)
    fun findPendingTasksAfter(sharingStateId: Long, limit: Int): List<PendingGoldenRecordTask> =
        sharingStateRepository.findBySharingStateTypeAndIdGreaterThanOrderByIdAsc(SharingStateType.Pending, sharingStateId, Pageable.ofSize(limit))
            .map { PendingGoldenRecordTask(sharingStateId = it.id, taskId = it.taskId) }

    /**
     * Returns up to [limit] successfully shared business partners with the golden records their output references, in
     * the order of their sharing state id and starting after [sharingStateId].
     */
    @Transactional(readOnly = true)
    fun findSharedAfter(sharingStateId: Long, limit: Int): List<SharedBusinessPartner> {
        val sharingStates = sharingStateRepository.findBySharingStateTypeAndIdGreaterThanOrderByIdAsc(SharingStateType.Success, sharingStateId, Pageable.ofSize(limit))
        val outputsBySharingStateId = businessPartnerRepository.findBySharingStateInAndStage(sharingStates, StageType.Output).associateBy { it.sharingState.id }
        return sharingStates.map { SharedBusinessPartner(it.id, outputsBySharingStateId[it.id]?.referencedBpns() ?: GoldenRecordBpns.NONE) }
    }

    /**
     * Sets the sharing state of each started creation in [creations] pending on its new task, and assigns every sharing
     * state in [creations] the Orchestrator record it is still missing.
     */
    @Transactional
    fun start(creations: List<GoldenRecordTaskCreationParsed>) {
        creations.forEach { creation ->
            val sharingState = creation.sharingState
            creation.recordIdToAssign?.let { sharingState.orchestratorRecordId = it }
            when (creation) {
                is GoldenRecordTaskCreationParsed.Started -> {
                    sharingState.sharingStateType = SharingStateType.Pending
                    sharingState.sharingErrorCode = null
                    sharingState.sharingErrorMessage = null
                    sharingState.sharingProcessStarted = sharingState.sharingProcessStarted ?: LocalDateTime.now()
                    sharingState.taskId = creation.taskId
                    sharingStateRepository.save(sharingState)
                    logChange(sharingState)
                    logger.debug { "Started golden record task ${creation.taskId} of business partner ${sharingState.externalId}" }
                }
                is GoldenRecordTaskCreationParsed.Stale -> if (creation.recordIdToAssign != null) {
                    sharingStateRepository.save(sharingState)
                    logChange(sharingState)
                }
            }
        }
    }

    /**
     * Marks each of [sharingStates] as successfully shared.
     */
    @Transactional
    fun succeed(sharingStates: List<SharingStateDb>) {
        sharingStates.forEach { sharingState ->
            sharingState.sharingStateType = SharingStateType.Success
            sharingState.sharingErrorCode = null
            sharingState.sharingErrorMessage = null
            sharingState.sharingProcessStarted = sharingState.sharingProcessStarted ?: LocalDateTime.now()
            sharingStateRepository.save(sharingState)
            logChange(sharingState)
            logger.debug { "Resolved golden record task ${sharingState.taskId} of business partner ${sharingState.externalId} as success" }
        }
    }

    /**
     * Marks [sharingState] as failed with [errorCode].
     */
    @Transactional
    fun fail(sharingState: SharingStateDb, errorCode: BusinessPartnerSharingErrorCode, errorMessage: String?) {
        sharingState.sharingStateType = SharingStateType.Error
        sharingState.sharingErrorCode = errorCode
        sharingState.sharingErrorMessage = errorMessage
        sharingState.sharingProcessStarted = sharingState.sharingProcessStarted ?: LocalDateTime.now()
        sharingStateRepository.save(sharingState)
        logChange(sharingState)
        logger.debug { "Resolved golden record task ${sharingState.taskId} of business partner ${sharingState.externalId} as error $errorCode" }
    }

    private fun create(externalId: String, tenantBpnl: String?): SharingStateDb {
        val sharingState = SharingStateDb(
            externalId,
            sharingStateType = initialType(),
            orchestratorRecordId = null,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            sharingProcessStarted = null,
            tenantBpnl = tenantBpnl,
            isGoldenRecordCounted = null,
            syncedIsGoldenRecordCounted = null
        )
        return sharingStateRepository.save(sharingState)
            .also { logChange(it, PersistedChangeType.Created) }
    }

    private fun initialType() =
        if (goldenRecordTaskConfigProperties.creation.fromSharingMember.startsAsReady) SharingStateType.Ready else SharingStateType.Initial

    private fun logChange(sharingState: SharingStateDb, type: PersistedChangeType = PersistedChangeType.Updated) {
        changeLogger.log(
            BusinessPartnerChange(type, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.SharingState)
        )
    }
}
