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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.RelationTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.service.gateway.RelationTaskGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationSharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sync.SyncRecordOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.task.RelationTaskResolutionOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.RelationTaskResolutionParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Resolves the relation golden record tasks the Orchestrator reports as finished into relation output and sharing
 * states.
 */
@Service
class RelationTaskResolutionApplicationService(
    private val taskGateway: RelationTaskGateway,
    private val resolutionParser: RelationTaskResolutionParser,
    private val sharingStateOperation: RelationSharingStateOperationService,
    private val resolutionOperation: RelationTaskResolutionOperationService,
    private val syncRecordOperation: SyncRecordOperationService,
    transactionManager: PlatformTransactionManager
) {

    // The remote reads stay outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${RelationTaskResolutionApplicationService::class.qualifiedName}.resolveFinishedTasks")
    }

    /**
     * Resolves every relation task finished since the previous resolution, committing one page of finished tasks at a
     * time.
     */
    fun resolveFinishedTasks() {
        do {
            val page = fetchNextPage()
            resolve(page)
        } while (page.finishedTasks.hasMore)
    }

    private fun fetchNextPage(): RelationTaskResolutionPageRequest {
        val finishedTasks = taskGateway.fetchFinishedTasks(syncRecordOperation.getSyncedUntil(SyncTypeDb.ORCHESTRATOR_FINISHED_RELATION_TASKS))
        val results = taskGateway.fetchResults(sharingStateOperation.findPendingTasks(finishedTasks.taskIds))
        return RelationTaskResolutionPageRequest(finishedTasks, results)
    }

    private fun resolve(page: RelationTaskResolutionPageRequest) {
        pageTransaction.executeWithoutResult {
            val resolutions = resolutionParser.parse(page).map { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> toFailed(result.errors)
                }
            }
            resolutionOperation.resolve(resolutions)
            syncRecordOperation.advance(SyncTypeDb.ORCHESTRATOR_FINISHED_RELATION_TASKS, page.finishedTasks.lastFinishedAt)
        }
    }

    private fun toFailed(errors: List<RelationTaskResolutionError>): RelationTaskResolutionParsed.Failed {
        val firstError = errors.first()
        return RelationTaskResolutionParsed.Failed(
            relation = firstError.relation,
            errorCode = toErrorCode(firstError),
            errorMessage = errors.joinToString { it.message }.take(250)
        )
    }

    private fun toErrorCode(error: RelationTaskResolutionError): RelationSharingStateErrorCode =
        when (error) {
            is RelationTaskResolutionError.TaskMissing -> RelationSharingStateErrorCode.SharingProcessError
            is RelationTaskResolutionError.ResultUnusable -> RelationSharingStateErrorCode.SharingProcessError
        }
}
