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

package org.eclipse.tractusx.bpdm.gate.service.application

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationError
import org.eclipse.tractusx.bpdm.gate.model.task.CreatedGoldenRecordTaskRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCreationParsed
import org.eclipse.tractusx.bpdm.gate.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.task.GoldenRecordTaskCreationOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.GoldenRecordTaskCreationParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Starts golden record tasks for the business partners ready to be shared.
 */
@Service
class GoldenRecordTaskCreationApplicationService(
    private val creationOperation: GoldenRecordTaskCreationOperationService,
    private val taskGateway: GoldenRecordTaskGateway,
    private val creationParser: GoldenRecordTaskCreationParser,
    private val sharingStateOperation: SharingStateOperationService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Orchestrator call stays outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordTaskCreationApplicationService::class.qualifiedName}.createTasksForReadyBusinessPartners")
    }

    /**
     * Starts a golden record task for every business partner ready to be shared, committing one page at a time until a
     * page starts none.
     */
    fun createTasksForReadyBusinessPartners() {
        do {
            val startedCount = createNextPage()
        } while (startedCount > 0)
    }

    private fun createNextPage(): Int {
        val candidates = creationOperation.findCandidates(taskProperties.creation.fromSharingMember.batchSize)
        if (candidates.isEmpty()) return 0

        return start(candidates, taskGateway.createTasks(candidates))
    }

    private fun start(candidates: List<GoldenRecordTaskCandidate>, createdTasks: List<CreatedGoldenRecordTaskRequest>): Int =
        pageTransaction.execute {
            val creations = creationParser.parse(candidates, createdTasks).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            creations.filterIsInstance<GoldenRecordTaskCreationParsed.Stale>().forEach(::log)

            sharingStateOperation.start(creations)
            creations.count { it is GoldenRecordTaskCreationParsed.Started }
        }

    private fun log(stale: GoldenRecordTaskCreationParsed.Stale) {
        logger.debug { "Left business partner '${stale.sharingState.externalId}' ready: it changed while its golden record task was being created" }
    }

    private fun log(error: GoldenRecordTaskCreationError) {
        when (error) {
            is GoldenRecordTaskCreationError.SharingStateMissing ->
                logger.debug { "Dropped golden record task ${error.taskId}: business partner '${error.externalId}' was deleted while the task was being created" }
            is GoldenRecordTaskCreationError.RecordIdInvalid ->
                logger.warn { "Left business partner '${error.externalId}' ready: the Orchestrator created task ${error.taskId} on the invalid record id '${error.recordId}'" }
        }
    }
}
