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
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskHealthCheckPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.PendingGoldenRecordTask
import org.eclipse.tractusx.bpdm.gate.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.task.GoldenRecordTaskHealthCheckOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.GoldenRecordTaskHealthCheckParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Fails the business partners pending on a golden record task the Orchestrator no longer knows.
 */
@Service
class GoldenRecordTaskHealthCheckApplicationService(
    private val taskGateway: GoldenRecordTaskGateway,
    private val healthCheckParser: GoldenRecordTaskHealthCheckParser,
    private val sharingStateOperation: SharingStateOperationService,
    private val healthCheckOperation: GoldenRecordTaskHealthCheckOperationService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Orchestrator call stays outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordTaskHealthCheckApplicationService::class.qualifiedName}.checkPendingTasks")
    }

    /**
     * Checks the task of every business partner pending on one, committing one page at a time.
     */
    fun checkPendingTasks() {
        var pendingTasks = findPendingTasksAfter(Long.MIN_VALUE)
        while (pendingTasks.isNotEmpty()) {
            checkPage(GoldenRecordTaskHealthCheckPageRequest(pendingTasks, taskGateway.fetchKnownTaskIds(pendingTasks.mapNotNull { it.taskId })))
            pendingTasks = findPendingTasksAfter(pendingTasks.last().sharingStateId)
        }
    }

    private fun findPendingTasksAfter(sharingStateId: Long): List<PendingGoldenRecordTask> =
        sharingStateOperation.findPendingTasksAfter(sharingStateId, taskProperties.healthCheck.batchSize)

    private fun checkPage(page: GoldenRecordTaskHealthCheckPageRequest) {
        pageTransaction.executeWithoutResult {
            val checks = healthCheckParser.parse(page).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            healthCheckOperation.failLostTasks(checks)
        }
    }

    private fun log(error: GoldenRecordTaskHealthCheckError) {
        when (error) {
            is GoldenRecordTaskHealthCheckError.SharingStateMissing ->
                logger.debug { "Skipped the health check of task ${error.taskId}: its business partner was deleted while the task was being checked" }
        }
    }
}
