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
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.union
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResolutionPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskResultRaw
import org.eclipse.tractusx.bpdm.gate.service.gateway.GoldenRecordTaskGateway
import org.eclipse.tractusx.bpdm.gate.service.gateway.PoolGoldenRecordGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sync.SyncRecordOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.task.GoldenRecordTaskResolutionOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.task.GoldenRecordTaskResolutionParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Resolves the golden record tasks the Orchestrator reports as finished into business partner output and sharing
 * states.
 */
@Service
class GoldenRecordTaskResolutionApplicationService(
    private val taskGateway: GoldenRecordTaskGateway,
    private val poolGateway: PoolGoldenRecordGateway,
    private val resolutionParser: GoldenRecordTaskResolutionParser,
    private val sharingStateOperation: SharingStateOperationService,
    private val resolutionOperation: GoldenRecordTaskResolutionOperationService,
    private val syncRecordOperation: SyncRecordOperationService,
    transactionManager: PlatformTransactionManager
) {

    // The remote reads stay outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordTaskResolutionApplicationService::class.qualifiedName}.resolveFinishedTasks")
    }

    /**
     * Resolves every task finished since the previous resolution, committing one page of finished tasks at a time.
     */
    fun resolveFinishedTasks() {
        do {
            val page = fetchNextPage()
            resolve(page)
        } while (page.finishedTasks.hasMore)
    }

    private fun fetchNextPage(): GoldenRecordTaskResolutionPageRaw {
        val finishedTasks = taskGateway.fetchFinishedTasks(syncRecordOperation.getSyncedUntil(SyncTypeDb.ORCHESTRATOR_FINISHED_TASKS))
        val results = taskGateway.fetchResults(sharingStateOperation.findPendingTasks(finishedTasks.taskIds))
        val succeededBpns = results.filterIsInstance<GoldenRecordTaskResultRaw.Succeeded>().map { it.bpns }
        return GoldenRecordTaskResolutionPageRaw(finishedTasks, results, poolGateway.fetchGoldenRecords(succeededBpns.union()))
    }

    private fun resolve(page: GoldenRecordTaskResolutionPageRaw) {
        pageTransaction.executeWithoutResult {
            val resolutions = resolutionParser.parse(page).flatMap { result ->
                when (result) {
                    is ParseResult.Success -> listOf(result.parsed)
                    is ParseResult.Failure -> result.errors.map(::toFailed)
                }
            }
            resolutionOperation.resolve(resolutions)
            syncRecordOperation.advance(SyncTypeDb.ORCHESTRATOR_FINISHED_TASKS, page.finishedTasks.lastFinishedAt)
        }
    }

    private fun toFailed(error: GoldenRecordTaskResolutionError): GoldenRecordTaskResolutionParsed.Failed {
        val errorCode = when (error) {
            is GoldenRecordTaskResolutionError.TaskMissing -> BusinessPartnerSharingErrorCode.MissingTaskID
            is GoldenRecordTaskResolutionError.InputMissing -> BusinessPartnerSharingErrorCode.SharingProcessError
            is GoldenRecordTaskResolutionError.ResultIncomplete -> BusinessPartnerSharingErrorCode.SharingProcessError
        }
        return GoldenRecordTaskResolutionParsed.Failed(error.sharingState, errorCode, error.message)
    }
}
