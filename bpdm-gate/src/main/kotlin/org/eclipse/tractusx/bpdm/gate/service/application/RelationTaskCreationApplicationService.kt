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
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskCreationError
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskCreationPageRequest
import org.eclipse.tractusx.bpdm.gate.service.gateway.RelationTaskGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationSharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.RelationTaskCreationParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Starts golden record tasks for the relations ready to be shared.
 */
@Service
class RelationTaskCreationApplicationService(
    private val sharingStateOperation: RelationSharingStateOperationService,
    private val preparationService: RelationTaskPreparationApplicationService,
    private val taskGateway: RelationTaskGateway,
    private val creationParser: RelationTaskCreationParser,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Orchestrator call stays outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${RelationTaskCreationApplicationService::class.qualifiedName}.createTasksForReadyRelations")
    }

    /**
     * Starts a golden record task for every relation ready to be shared when the run begins, committing one page at a
     * time and leaving relations whose endpoints are not yet shared ready for a later run.
     */
    fun createTasksForReadyRelations() {
        sharingStateOperation.stageReady()
        do {
            val preparation = preparationService.prepareStaged(taskProperties.relationCreation.batchSize)
            if (preparation.candidates.isNotEmpty())
                start(RelationTaskCreationPageRequest(preparation.candidates, taskGateway.createTasks(preparation.candidates)))
        } while (preparation.preparedCount > 0)
    }

    private fun start(page: RelationTaskCreationPageRequest) {
        pageTransaction.executeWithoutResult {
            val creations = creationParser.parse(page).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            sharingStateOperation.start(creations)
        }
    }

    private fun log(error: RelationTaskCreationError) {
        when (error) {
            is RelationTaskCreationError.RelationMissing ->
                logger.debug { "Dropped golden record task ${error.taskId}: relation '${error.externalId}' was deleted while the task was being created" }
            is RelationTaskCreationError.RelationChanged ->
                logger.debug { "Dropped golden record task ${error.taskId}: relation '${error.externalId}' changed while the task was being created" }
        }
    }
}
