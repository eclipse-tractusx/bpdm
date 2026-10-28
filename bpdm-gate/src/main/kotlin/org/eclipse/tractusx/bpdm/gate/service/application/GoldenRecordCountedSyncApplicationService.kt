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
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedSyncError
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedUpdate
import org.eclipse.tractusx.bpdm.gate.service.gateway.SharingMemberRecordGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.GoldenRecordCountedOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.sharingstate.GoldenRecordCountedSyncParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Sends the Orchestrator which business partners count towards the sharing member count of their golden record.
 */
@Service
class GoldenRecordCountedSyncApplicationService(
    private val sharingMemberRecordGateway: SharingMemberRecordGateway,
    private val syncParser: GoldenRecordCountedSyncParser,
    private val countedOperation: GoldenRecordCountedOperationService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Orchestrator call stays outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordCountedSyncApplicationService::class.qualifiedName}.synchronizeGoldenRecordCounted")
    }

    /**
     * Sends every golden record counted flag the Orchestrator has not received yet, committing one page at a time.
     */
    fun synchronizeGoldenRecordCounted() {
        var updates = findUnsyncedAfter(Long.MIN_VALUE)
        while (updates.isNotEmpty()) {
            sharingMemberRecordGateway.updateGoldenRecordCounted(updates)
            markSynced(updates)
            updates = findUnsyncedAfter(updates.last().sharingStateId)
        }
    }

    private fun findUnsyncedAfter(sharingStateId: Long): List<GoldenRecordCountedUpdate> =
        countedOperation.findUnsyncedAfter(sharingStateId, taskProperties.recordSync.batchSize)

    private fun markSynced(updates: List<GoldenRecordCountedUpdate>) {
        pageTransaction.executeWithoutResult {
            val syncs = syncParser.parse(updates).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            countedOperation.markSynced(syncs)
        }
    }

    private fun log(error: GoldenRecordCountedSyncError) {
        when (error) {
            is GoldenRecordCountedSyncError.SharingStateMissing ->
                logger.debug { "Skipped recording the golden record counted flag of sharing state ${error.sharingStateId} as received: its business partner was deleted while the flag was being sent" }
        }
    }
}
