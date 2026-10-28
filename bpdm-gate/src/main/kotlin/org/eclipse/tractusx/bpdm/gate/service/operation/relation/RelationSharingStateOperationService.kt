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

package org.eclipse.tractusx.bpdm.gate.service.operation.relation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationChange
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskCreationParsed
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskPreparationParsed
import org.eclipse.tractusx.bpdm.gate.model.task.TaskReference
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Moves the sharing states of relations through their lifecycle.
 */
@Service
class RelationSharingStateOperationService(
    private val relationRepository: RelationRepository,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Resets the sharing state of [relation] to ready for a new sharing attempt, keeping only its Orchestrator record.
     */
    @Transactional
    fun reset(relation: RelationDb) {
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Ready,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            recordId = relation.sharingState?.recordId,
            taskId = null,
            updatedAt = Instant.now(),
            isStaged = false
        )
        relationRepository.save(relation)
    }

    /**
     * Stages every relation ready to be shared for the golden record task creation, so that a creation run covers the
     * relations ready when it began and no others.
     */
    @Transactional
    fun stageReady() {
        val stagedCount = relationRepository.stageBySharingStateType(RelationSharingStateType.Ready)
        logger.debug { "Staged $stagedCount relations ready to be shared" }
    }

    /**
     * Returns up to [limit] relations staged for sharing.
     */
    @Transactional(readOnly = true)
    fun findStaged(limit: Int): List<RelationDb> =
        relationRepository.findBySharingStateAndStaged(RelationSharingStateType.Ready, true, Pageable.ofSize(limit)).content

    /**
     * Sets the sharing state of each failed preparation in [preparations] to error, and unstages each deferred
     * preparation so that it stays ready for a later run.
     */
    @Transactional
    fun prepare(preparations: List<RelationTaskPreparationParsed>) {
        preparations.forEach { preparation ->
            val relation = preparation.relation
            when (preparation) {
                is RelationTaskPreparationParsed.Sendable -> Unit
                is RelationTaskPreparationParsed.Deferred -> {
                    checkNotNull(relation.sharingState).isStaged = false
                    relationRepository.save(relation)
                    logger.debug { "Deferred the golden record task of relation ${relation.externalId}: its endpoints are not yet shared" }
                }
                is RelationTaskPreparationParsed.Failed -> {
                    markError(relation, preparation.errorCode, preparation.errorMessage)
                    logger.debug { "Failed to prepare a golden record task for relation ${relation.externalId}: ${preparation.errorMessage}" }
                }
            }
        }
    }

    /**
     * Sets the sharing state of each of [creations] pending on its new task.
     */
    @Transactional
    fun start(creations: List<RelationTaskCreationParsed>) {
        creations.forEach { creation ->
            val relation = creation.relation
            relation.sharingState = RelationSharingStateDb(
                sharingStateType = RelationSharingStateType.Pending,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                recordId = creation.recordId,
                taskId = creation.taskId,
                updatedAt = Instant.now(),
                isStaged = false
            )
            relationRepository.save(relation)
            logChange(relation)
            logger.debug { "Started golden record task ${creation.taskId} of relation ${relation.externalId}" }
        }
    }

    /**
     * Returns a reference to the task of each relation still pending on any of [taskIds].
     */
    @Transactional(readOnly = true)
    fun findPendingTasks(taskIds: Collection<String>): List<TaskReference> =
        relationRepository.findBySharingStateTaskIdIn(taskIds.toSet())
            .mapNotNull { it.sharingState }
            .filter { it.sharingStateType == RelationSharingStateType.Pending }
            .map { TaskReference(taskId = checkNotNull(it.taskId), recordId = checkNotNull(it.recordId)) }

    /**
     * Marks each of [relations] as successfully shared.
     */
    @Transactional
    fun succeed(relations: List<RelationDb>) {
        relations.forEach { relation ->
            relation.sharingState = RelationSharingStateDb(
                sharingStateType = RelationSharingStateType.Success,
                sharingErrorCode = null,
                sharingErrorMessage = null,
                recordId = relation.sharingState?.recordId,
                taskId = relation.sharingState?.taskId,
                updatedAt = Instant.now(),
                isStaged = false
            )
            relationRepository.save(relation)
            logChange(relation)
            logger.debug { "Resolved golden record task ${relation.sharingState?.taskId} of relation ${relation.externalId} as success" }
        }
    }

    /**
     * Marks [relation] as failed with [errorCode].
     */
    @Transactional
    fun fail(relation: RelationDb, errorCode: RelationSharingStateErrorCode, errorMessage: String) {
        markError(relation, errorCode, errorMessage)
        logger.debug { "Resolved golden record task ${relation.sharingState?.taskId} of relation ${relation.externalId} as error $errorCode" }
    }

    private fun markError(relation: RelationDb, errorCode: RelationSharingStateErrorCode, errorMessage: String) {
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Error,
            sharingErrorCode = errorCode,
            sharingErrorMessage = errorMessage,
            recordId = relation.sharingState?.recordId,
            taskId = relation.sharingState?.taskId,
            updatedAt = Instant.now(),
            isStaged = false
        )
        relationRepository.save(relation)
        logChange(relation)
    }

    private fun logChange(relation: RelationDb) {
        changeLogger.log(RelationChange(PersistedChangeType.Updated, relation.externalId, relation.tenantBpnL, RelationChange.Aspect.SharingState))
    }
}
