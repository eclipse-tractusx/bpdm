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
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.union
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionPageRequest
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskResultRequest
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
    private val logger = KotlinLogging.logger { }

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

    private fun fetchNextPage(): GoldenRecordTaskResolutionPageRequest {
        val finishedTasks = taskGateway.fetchFinishedTasks(syncRecordOperation.getSyncedUntil(SyncTypeDb.ORCHESTRATOR_FINISHED_TASKS))
        val results = taskGateway.fetchResults(sharingStateOperation.findPendingTasks(finishedTasks.taskIds))
        val succeededBpns = results.filterIsInstance<GoldenRecordTaskResultRequest.Succeeded>().map { it.bpns }
        return GoldenRecordTaskResolutionPageRequest(finishedTasks, results, poolGateway.fetchGoldenRecords(succeededBpns.union()))
    }

    private fun resolve(page: GoldenRecordTaskResolutionPageRequest) {
        pageTransaction.executeWithoutResult {
            val resolutions = resolutionParser.parse(page).flatMap { result ->
                when (result) {
                    is ParseResult.Success -> listOf(result.parsed)
                    is ParseResult.Failure -> result.errors.map(::toFailed)
                }
            }
            resolutions.filterIsInstance<GoldenRecordTaskResolutionParsed.Succeeded>()
                .forEach { it.alignmentErrors.forEach(::log) }

            resolutionOperation.resolve(resolutions)
            syncRecordOperation.advance(SyncTypeDb.ORCHESTRATOR_FINISHED_TASKS, page.finishedTasks.lastFinishedAt)
        }
    }

    private fun toFailed(error: GoldenRecordTaskResolutionError): GoldenRecordTaskResolutionParsed.Failed {
        val errorCode = when (error) {
            is GoldenRecordTaskResolutionError.TaskMissing -> BusinessPartnerSharingError.MissingTaskID
            is GoldenRecordTaskResolutionError.InputMissing -> BusinessPartnerSharingError.SharingProcessError
            is GoldenRecordTaskResolutionError.ResultIncomplete -> BusinessPartnerSharingError.SharingProcessError
        }
        return GoldenRecordTaskResolutionParsed.Failed(error.sharingState, errorCode, error.message)
    }

    private fun log(error: GoldenRecordAlignmentError) {
        val externalId = error.sharingState.externalId
        when (error) {
            is GoldenRecordAlignmentError.GoldenRecordMissing ->
                logger.debug { "Kept the task result of business partner output '$externalId' unaligned: the Pool does not return golden record '${error.bpn}'" }
            is GoldenRecordAlignmentError.AdoptedSiteMissing ->
                logger.warn { "Kept the task result of business partner output '$externalId' unaligned: its address belongs to site '${error.siteBpn}' which the Pool does not return" }
        }
    }
}
